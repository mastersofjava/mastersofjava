package nl.moj.worker.java.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.messages.JMSTestCase;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import nl.moj.worker.java.ClasspathService;
import nl.moj.worker.workspace.Workspace;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.listener.ProcessListener;

import java.io.Closeable;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestRunnerService {

    public static final String SECURITY_POLICY_FOR_UNIT_TESTS = "securityPolicyForUnitTests.policy";
    private static final Pattern JUNIT_PREFIX_P = Pattern.compile("^(JUnit version 4.12)?\\s*\\.?",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final MojServerProperties mojServerProperties;
    private final ClasspathService classpathService;

    public TestOutput test(Workspace workspace, JMSTestCase test, UUID runId) {
        TestOutput to = TestOutput.builder()
                .runId(runId)
                .dateTimeStart(Instant.now())
                .build();

        log.info("Test case {} {} starting.", test.getTestCase(), test.getName());

        try (Closeable ignored = MDC.putCloseable("run", runId.toString())) {
            AssignmentDescriptor ad = workspace.getAssignmentDescriptor();
            Path policy = resolveSecurityPolicy(ad);

            Duration timeout = ad.getTestTimeout() != null ? ad.getTestTimeout()
                    : mojServerProperties.getLimits().getTestTimeout();

            if (!policy.toFile().exists()) {
                log.info("Test case {} {} missing security policy, aborting.", test.getTestCase(), test.getName());
                return TestOutput.builder()
                        .aborted(true)
                        .reason("No security policy defined.")
                        .build();
            }

            try (final LengthLimitedOutputCatcher jUnitOutput = new LengthLimitedOutputCatcher(
                    mojServerProperties.getLimits().getTestOutputLimits());
                 final LengthLimitedOutputCatcher jUnitError = new LengthLimitedOutputCatcher(
                         mojServerProperties.getLimits().getTestOutputLimits())) {

                try {
                    List<String> cmd = new ArrayList<>();
                    cmd.add(
                            mojServerProperties.getLanguages().getJavaVersion(ad.getJavaVersion()).getRuntime().toString());
                    if (ad.getJavaVersion() > 11) {
                        cmd.add("--enable-preview");
                    }
                    cmd.add("-cp");
                    cmd.add(classpathService.resolveClasspath(List.of(workspace.getTargetRoot())));
                    cmd.add("-Djava.security.manager");
                    cmd.add("-Djava.security.policy=" + policy.toAbsolutePath());
                    cmd.add("org.junit.runner.JUnitCore");

                    // this expects a class name
                    cmd.add(test.getName());

                    final ProcessResult pr = new ProcessExecutor()
                            .command(cmd)
                            .directory(workspace.getRoot().toFile())
                            .timeout(timeout.toSeconds(), TimeUnit.SECONDS)
                            .redirectOutput(jUnitOutput)
                            .redirectError(jUnitError)
                            .addListener(new ProcessListener() {
                                @Override
                                public void afterStart(Process process, ProcessExecutor executor) {
                                    log.info("Test case {} {} executing: {}", test.getTestCase(), test.getName(), process.info().commandLine().orElse("<none>"));
                                }
                            }).execute();
                    to.setSuccess(pr.getExitValue() == 0);
                    to.setDateTimeEnd(Instant.now());

                } catch (TimeoutException e) {
                    log.info("Test case {} {} timed out.", test.getTestCase(), test.getName());
                    to.setTimedOut(true);
                    jUnitOutput.getBuffer().append('\n')
                            .append(mojServerProperties.getLimits().getTestOutputLimits().getTimeoutMessage());
                } catch (SecurityException se) {
                    log.info("Test case {} {} aborted.", test.getTestCase(), test.getName(), se);
                    to.setAborted(true);
                    to.setReason(se.getMessage());
                }

                to.setOutput(stripJUnitPrefix(jUnitOutput.getBuffer()));
                to.setErrorOutput(stripJUnitPrefix(jUnitError.getBuffer()));

                log.info("Test case {} {} finished.", test.getTestCase(), test.getName());
                return to;
            }
        } catch (Throwable e) {
            log.error("Unexpected exception running test case {} {}, aborting", test.getTestCase(), test.getName(), e);
            return TestOutput.builder()
                    .dateTimeStart(to.getDateTimeStart())
                    .dateTimeEnd(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build();
        }
    }

    private Path resolveSecurityPolicy(AssignmentDescriptor ad) {
        Path policy = ad.getAssignmentFiles().getSecurityPolicy();

        if (policy != null) {
            policy = ad.getDirectory().resolve(policy);
        } else {
            policy = mojServerProperties.getDirectories().getBaseDirectory()
                    .resolve(mojServerProperties.getDirectories().getLibDirectory())
                    .resolve(SECURITY_POLICY_FOR_UNIT_TESTS);
        }
        return policy;
    }

    private String stripJUnitPrefix(StringBuilder result) {
        final Matcher matcher = JUNIT_PREFIX_P.matcher(result);
        if (matcher.find()) {
            result.delete(0, matcher.end());
            if (result.length() > 0 && result.charAt(0) == '\n') {
                result.deleteCharAt(0);
            }
        }
        return result.toString();
    }

}
