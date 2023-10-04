package nl.moj.worker.java.test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.messages.JMSTestCase;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.common.storage.StorageService;
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
public class TestRunnerService {

    public static final String SECURITY_POLICY_FOR_UNIT_TESTS = "securityPolicyForUnitTests.policy";

    private static  final Pattern JUNIT_PREFIX_P = Pattern.compile("^JUnit version.*$|^\\.$|^I$|^E$",
            Pattern.MULTILINE);
    private static final Pattern WARN_SECURITY_MANAGER = Pattern.compile("^WARNING:.+Security Manager.*$",
            Pattern.MULTILINE);

    private final MojServerProperties mojServerProperties;
    private final ClasspathService classpathService;

    private final StorageService storageService;

    public TestCaseOutput test(Workspace workspace, JMSTestCase test) {
        TestCaseOutput to = TestCaseOutput.builder()
                .testCase(test.getTestCase())
                .dateTimeStart(Instant.now())
                .build();

        log.info("Test case {} {} starting.", test.getTestCase(), test.getName());

        try {
            AssignmentDescriptor ad = workspace.getAssignmentDescriptor();
            Path policy = resolveSecurityPolicy(ad);

            Duration timeout = ad.getTestTimeout() != null ? ad.getTestTimeout()
                    : mojServerProperties.getLimits().getTestTimeout();

            if (!policy.toFile().exists()) {
                log.info("Test case {} {} missing security policy, aborting.", test.getTestCase(), test.getName());
                return TestCaseOutput.builder()
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
                            mojServerProperties.getLanguages()
                                    .getJavaVersion(ad.getJavaVersion())
                                    .getRuntime()
                                    .toString());
                    if (ad.getJavaVersion() > 11) {
                        cmd.add("--enable-preview");
                    }
                    cmd.add("-cp");
                    cmd.add(classpathService.resolveClasspath(List.of(workspace.getTargetRoot())));
                    cmd.add("-Djava.security.manager");
                    cmd.add("-Djava.security.policy=" + policy.toAbsolutePath());
                    cmd.addAll(resolveSystemProperties(ad));
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
                                    log.info("Test case {} {} executing: {}", test.getTestCase(), test.getName(), process.info()
                                            .commandLine()
                                            .orElse("<none>"));
                                }
                            }).execute();
                    to.setSuccess(pr.getExitValue() == 0);
                    to.setDateTimeEnd(Instant.now());

                } catch (TimeoutException e) {
                    log.info("Test case {} {} timed out.", test.getTestCase(), test.getName());
                    to.setTimedOut(true);
                    to.setDateTimeEnd(Instant.now());
                    to.setReason("Compiling timed out.");
                    jUnitOutput.getBuffer().append('\n')
                            .append(mojServerProperties.getLimits().getTestOutputLimits().getTimeoutMessage());
                } catch (SecurityException se) {
                    log.info("Test case {} {} aborted.", test.getTestCase(), test.getName(), se);
                    to.setAborted(true);
                    to.setDateTimeEnd(Instant.now());
                    to.setReason("Testing triggered security policy violation.");
                    to.setReason(se.getMessage());
                }

                to.setOutput(cleanupOutput(jUnitOutput.getBuffer()));
                to.setErrorOutput(cleanupOutput(jUnitError.getBuffer()));

                log.info("Test case {} {} finished.", test.getTestCase(), test.getName());
                return to;
            }
        } catch (Throwable e) {
            log.error("Unexpected exception running test case {} {}, aborting", test.getTestCase(), test.getName(), e);
            return TestCaseOutput.builder()
                    .testCase(test.getTestCase())
                    .dateTimeStart(to.getDateTimeStart())
                    .dateTimeEnd(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build();
        }
    }

    private List<String> resolveSystemProperties(AssignmentDescriptor ad) {
        List<String> systemProperties = new ArrayList<>();
        if( ad.getSystemProperties() !=  null ) {
            ad.getSystemProperties().forEach((k, v) -> {
                String rv = v;
                if( rv.contains("${base}/")) {
                    rv = ad.getDirectory().resolve(v.replace("${base}/", "")).toAbsolutePath().toString();
                }
                systemProperties.add(String.format("-D%s=%s",k,rv));
            });
        }
        return systemProperties;
    }

    private Path resolveSecurityPolicy(AssignmentDescriptor ad) {
        Path policy = ad.getAssignmentFiles().getSecurityPolicy();

        if (policy != null) {
            policy = ad.getDirectory().resolve(policy);
        } else {
            policy = storageService.getLibsFolder()
                    .resolve(SECURITY_POLICY_FOR_UNIT_TESTS);
        }
        return policy;
    }

    private String cleanupOutput(StringBuilder result) {
        Matcher matcher = JUNIT_PREFIX_P.matcher(result);
        result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, "");
        }
        matcher.appendTail(result);

        // strip security manager warnings
        matcher = WARN_SECURITY_MANAGER.matcher(result);
        result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, "");
        }
        matcher.appendTail(result);
        return result.toString().trim();
    }
}
