package nl.moj.worker.java.compile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.worker.util.LengthLimitedOutputCatcher;
import nl.moj.worker.java.ClasspathService;
import nl.moj.worker.workspace.Workspace;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.listener.ProcessListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompileRunnerService {

    private final MojServerProperties mojServerProperties;
    private final ClasspathService classpathService;

    public CompileOutput compile(Workspace workspace) {

        CompileOutput co = CompileOutput.builder()
                .dateTimeStart(Instant.now())
                .build();

        log.info("Compile starting.");

        try {
            // find java compiler
            AssignmentDescriptor ad = workspace.getAssignmentDescriptor();
            var javaVersion = mojServerProperties.getLanguages().getJavaVersion(ad.getJavaVersion());

            // configure output limits
            final LengthLimitedOutputCatcher compileOutput = new LengthLimitedOutputCatcher(
                    mojServerProperties.getLimits().getCompileOutputLimits());
            final LengthLimitedOutputCatcher compileErrorOutput = new LengthLimitedOutputCatcher(
                    mojServerProperties.getLimits().getCompileOutputLimits());
            final Duration timeout = ad.getCompileTimeout() != null ? ad.getCompileTimeout()
                    : mojServerProperties.getLimits().getCompileTimeout();

            // build and run javac command
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
                cmd.add(classpathService.resolveClasspath(Set.of(workspace.getTargetRoot())));
                cmd.add("-d");
                cmd.add(workspace.getTargetRoot().toAbsolutePath().toString());
                try (Stream<Path> sources = workspace.getSources()) {
                    sources.forEach(s -> {
                        if (!Files.isDirectory(s)) {
                            if (!Files.exists(s)) {
                                throw new IllegalStateException("Source file " + s + " does not exist in workspace, though was found listing it.");
                            }
                            cmd.add(s.toAbsolutePath().toString());
                        }
                    });
                }

                long closeTimeout = timeout.toSeconds() + 4;

                final ProcessResult processResult = new ProcessExecutor(cmd)
                        .destroyOnExit().closeTimeout(closeTimeout, TimeUnit.SECONDS)
                        .directory(workspace.getRoot().toFile())
                        .timeout(timeout.toSeconds(), TimeUnit.SECONDS).redirectOutput(compileOutput)
                        .redirectError(compileErrorOutput)
                        .addListener(new ProcessListener() {
                            @Override
                            public void afterStart(Process process, ProcessExecutor executor) {
                                log.info("Executed: {}", process.info().commandLine().orElse("<none>"));
                            }
                        }).execute();
                co.setSuccess(processResult.getExitValue() == 0);

//                InputStream is = commandExecutor.pumps().getInput();
//                OutputStream error = commandExecutor.pumps().getErr();
//                OutputStream out = commandExecutor.pumps().getOut();
//                commandExecutor.pumps().flush();
//
//                if (is != null) {
//                    is.close();
//                }
//                if (error != null) {
//                    error.close();
//                }
//                if (out != null) {
//                    out.close();
//                }
//                commandExecutor.pumps().stop();
                co.setDateTimeEnd(Instant.now());

            } catch (TimeoutException e) {
                // process is automatically destroyed
                co.setTimedOut(true);
                co.setDateTimeEnd(Instant.now());
                co.setReason("Compiling timed out.");
                compileOutput.getBuffer()
                        .append(mojServerProperties.getLimits().getCompileOutputLimits().getTimeoutMessage());
            }

            co.setOutput(stripTeamPathInfo(compileOutput.getBuffer(), workspace.getSourcesRoot()));
            co.setErrorOutput(stripTeamPathInfo(compileErrorOutput.getBuffer(), workspace.getSourcesRoot()));

            log.info("Compile finished.");

            return co;

        } catch (Throwable e) {
            log.info("Unexpected exception running compile, aborting", e);
            return CompileOutput.builder()
                    .dateTimeStart(co.getDateTimeStart())
                    .dateTimeEnd(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build();
        }
    }

    private String stripTeamPathInfo(StringBuilder result, Path prefix) {
        if (result != null) {
            return result.toString().replace(prefix.toAbsolutePath() + File.separator, "");
        }
        return "";
    }
}
