package nl.moj.worker.compile.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.messages.JMSFile;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.common.messages.JMSCompileRequest;
import nl.moj.server.runtime.service.RuntimeService;
import nl.moj.server.submit.service.ExecutionService;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import nl.moj.worker.workspace.service.Workspace;
import nl.moj.worker.workspace.service.WorkspaceService;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.listener.ProcessListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompileRunnerService {

    private final MojServerProperties mojServerProperties;
    private final ExecutionService executionService;
    private final WorkspaceService workspaceService;
    private final RuntimeService runtimeService;

    public CompletableFuture<CompileResult> compile(JMSCompileRequest jmsCompileRequest) throws IOException {
        AssignmentDescriptor ad = runtimeService.getAssignmentDescriptor(jmsCompileRequest.getAssignment());
        return CompletableFuture.supplyAsync(() -> {
            CompileResult result = CompileResult.builder()
                    .compileAttemptUuid(jmsCompileRequest.getAttempt()).build();
            Map<Path,String> sources = jmsCompileRequest.getSources().stream().collect(Collectors.toMap( s -> Paths.get(s.getPath()), JMSFile::getContent) );
            try (Workspace workspace = workspaceService.getWorkspace(ad, sources)) {
                CompileOutput co = javaCompile(workspace, jmsCompileRequest.getAttempt());

                if (co.getExitvalue() == 0 && !co.isTimedOut()) {
                    result = result.toBuilder()
                            .success(true)
                            .compileOutput(concat(co.getOutput(),co.getErrorOutput()))
                            .timeout(false)
                            .build();
                } else {
                    result = result.toBuilder()
                            .success(false)
                            .compileOutput(concat(co.getOutput(),co.getErrorOutput()))
                            .timeout(co.isTimedOut())
                            .build();
                }
                return result.toBuilder()
                        .dateTimeStart(co.getDateTimeStart())
                        .dateTimeEnd(co.getDateTimeEnd())
                        .build();

            } catch (Exception e) {
                log.error("Compile for attempt {} failed.",jmsCompileRequest.getAttempt(),e);
                return result.toBuilder()
                        .aborted(true)
                        .reason(e.getMessage())
                        .build();
            }
        }, executionService.getExecutor(ad));
    }

    private String concat(String a, String b) {
        StringBuilder sb = new StringBuilder();
        if( a != null && b.length() > 0) {
            sb.append(a);
        }
        if( sb.length() > 0 && b != null && b.length() > 0 ) {
            sb.append("\n\n");
        }
        if( b != null && b.length() > 0 ) {
            sb.append(b);
        }
        return sb.toString();
    }

    // TODO this will be client side
    private CompileOutput javaCompile(Workspace workspace, UUID attempt) {
        try {

            // find java compiler
            AssignmentDescriptor ad = workspace.getAssignmentDescriptor();
            var javaVersion = mojServerProperties.getLanguages().getJavaVersion(ad.getJavaVersion());

            // C) Java compiler options
            boolean timedOut = false;
            int exitvalue = 0;
            final LengthLimitedOutputCatcher compileOutput = new LengthLimitedOutputCatcher(
                    mojServerProperties.getLimits().getCompileOutputLimits());
            final LengthLimitedOutputCatcher compileErrorOutput = new LengthLimitedOutputCatcher(
                    mojServerProperties.getLimits().getCompileOutputLimits());
            final Duration timeout = ad.getCompileTimeout() != null ? ad.getCompileTimeout()
                    : mojServerProperties.getLimits().getCompileTimeout();

            final Instant dateTimeStart = Instant.now();

            try {
                List<String> cmd = new ArrayList<>();
                cmd.add(javaVersion.getCompiler().toString());
                cmd.add("-Xlint:all");

                boolean enablePreviewFeatures = ad.isJavaPreviewEnabled();
                if (javaVersion.getVersion() >= 11 && enablePreviewFeatures) {
                    cmd.add("--enable-preview");
                    cmd.add("--release");
                    cmd.add("" + javaVersion.getVersion());
                }

                cmd.add("-encoding");
                cmd.add("UTF8");
                cmd.add("-g:source,lines,vars");
                cmd.add("-cp");
                cmd.add(makeClasspath(workspace.getTargetRoot()).stream().map( p -> p.toAbsolutePath().toString())
                        .collect(Collectors.joining(File.pathSeparator)));
                cmd.add("-d");
                cmd.add(workspace.getTargetRoot().toAbsolutePath().toString());
                try (Stream<Path> sources = workspace.getSources()) {
                    sources.forEach(s -> {
                        if( !Files.isDirectory(s)) {
                            if (!Files.exists(s)) {
                                throw new IllegalStateException("Source file " + s + " does not exist in workspace, though was found listing it.");
                            }
                            cmd.add(s.toAbsolutePath().toString());
                        }
                    });
                }

                long closeTimeout = timeout.toSeconds() + 4;

                final ProcessExecutor commandExecutor = new ProcessExecutor(cmd); //.command(cmd);
                commandExecutor.destroyOnExit().closeTimeout(closeTimeout, TimeUnit.SECONDS)
                        .directory(workspace.getRoot().toFile())
                        .timeout(timeout.toSeconds(), TimeUnit.SECONDS).redirectOutput(compileOutput)
                        .redirectError(compileErrorOutput)
                        .addListener(new ProcessListener() {
                            @Override
                            public void afterStart(Process process, ProcessExecutor executor) {
                                log.info("Executed: {}",process.info().commandLine().orElse("<none>"));
                            }
                        });

                log.debug("Executing command {}", String.join(" \\\n", cmd));
                ProcessResult processResult = commandExecutor.execute();
                exitvalue = processResult.getExitValue();

                InputStream is = commandExecutor.pumps().getInput();
                OutputStream error = commandExecutor.pumps().getErr();
                OutputStream out = commandExecutor.pumps().getOut();
                commandExecutor.pumps().flush();
                if (is != null) {
                    is.close();
                }
                if (error != null) {
                    error.close();
                }
                if (out != null) {
                    out.close();
                }
                log.debug("commandExecutor stop ");
                commandExecutor.pumps().stop();

            } catch (TimeoutException e) {
                // process is automatically destroyed
                log.debug("Compile timed out and got killed for attempt {}.", attempt);
                timedOut = true;
            } catch (SecurityException se) {
                log.error(se.getMessage(), se);
            }
            log.debug("exitValue {}, timeoutConfiguration {} ", exitvalue, timeout.toSeconds());
            if (timedOut) {
                compileOutput.getBuffer().append('\n')
                        .append(mojServerProperties.getLimits().getCompileOutputLimits().getTimeoutMessage());
            }

            String output = stripTeamPathInfo(compileOutput.getBuffer(), workspace.getSourcesRoot());
            String errorOutput = stripTeamPathInfo(compileErrorOutput.getBuffer(), workspace.getSourcesRoot());

            return new CompileOutput(output, errorOutput, exitvalue, timedOut,
                    dateTimeStart, Instant.now());
        } catch (Exception e) {
            log.error("Unable to compile the assignment due to unexpected exception: {}", e.getMessage(), e);
            return new CompileOutput("", "Unable to compile: " + e.getMessage(), -1, false, Instant.now(),
                    Instant.now());
        }
    }

    private String stripTeamPathInfo(StringBuilder result, Path prefix) {
        if (result != null) {
            return result.toString().replace(prefix.toAbsolutePath() + File.separator, "");
        }
        return "";
    }

    private List<Path> makeClasspath(Path classesDir) {
        final List<Path> classPath = new ArrayList<>();
        classPath.add(classesDir);
        classPath.add(resolveLibrary("junit-4.12.jar"));
        classPath.add(resolveLibrary("hamcrest-all-1.3.jar"));
        classPath.add(resolveLibrary("asciiart-core-1.1.0.jar"));

        for (Path file : classPath) {
            if (Files.exists(file)) {
                log.error("not found: {}", file.toAbsolutePath());
            } else {
                log.trace("found: {}", file.toAbsolutePath());
            }
        }
        return classPath;
    }

    private Path resolveLibrary(String library) {
        return mojServerProperties.getDirectories().getBaseDirectory()
                .resolve(mojServerProperties.getDirectories().getLibDirectory()).resolve(library );
    }
}
