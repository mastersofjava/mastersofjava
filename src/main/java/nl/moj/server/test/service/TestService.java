package nl.moj.server.test.service;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestService {
    public static final String SECURITY_POLICY_FOR_UNIT_TESTS = "securityPolicyForUnitTests.policy";
    private static final Logger log = LoggerFactory.getLogger(TestService.class);
    private static final Pattern JUNIT_PREFIX_P = Pattern.compile("^(JUnit version 4.12)?\\s*\\.?",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private Executor executor;

    private MojServerProperties mojServerProperties;

    private CompetitionRuntime competition;

    private TestCaseRepository testCaseRepository;

    private TestAttemptRepository testAttemptRepository;

    private AssignmentStatusRepository assignmentStatusRepository;

    public TestService(MojServerProperties mojServerProperties, @Qualifier("testing") Executor executor, CompetitionRuntime competition,
                       TestCaseRepository testCaseRepository, TestAttemptRepository testAttemptRepository,
                       AssignmentStatusRepository assignmentStatusRepository) {
        this.mojServerProperties = mojServerProperties;
        this.executor = executor;
        this.competition = competition;
        this.testCaseRepository = testCaseRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.assignmentStatusRepository = assignmentStatusRepository;
    }

    public CompletableFuture<TestResult> runTest(Team team, TestAttempt testAttempt, AssignmentFile test) {
        return CompletableFuture.supplyAsync(() -> executeTest(team, testAttempt, test, competition.getActiveAssignment()
                .getAssignmentDescriptor()), executor);
    }

    private TestResult runTestSync(Team team, TestAttempt testAttempt, AssignmentFile test) {
        return executeTest(team, testAttempt, test, competition.getActiveAssignment().getAssignmentDescriptor());
    }

    public TestResults runTestsSync(Team team, List<AssignmentFile> tests) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment.getAssignment(),
                activeAssignment.getCompetitionSession(), team);
        TestAttempt ta = testAttemptRepository.save(TestAttempt.builder()
                .assignmentStatus(as)
                .dateTimeStart(Instant.now())
                .uuid(UUID.randomUUID())
                .build());

        List<TestResult> trs = new ArrayList<>();
        tests.forEach(t -> trs.add(runTestSync(team, ta, t)));

        ta.setDateTimeEnd(Instant.now());
        testAttemptRepository.save(ta);

        return TestResults.builder()
                .testAttemptUuid(ta.getUuid())
                .results(trs)
                .build();
    }

    private TestResult executeTest(Team team, TestAttempt testAttempt, AssignmentFile file, AssignmentDescriptor ad) {
        log.info("Running unit test: {}", file.getName());

        TestCase testCase = TestCase.builder()
                .testAttempt(testAttempt)
                .name(file.getName())
                .uuid(UUID.randomUUID())
                .dateTimeStart(Instant.now())
                .build();

        File teamdir = FileUtils.getFile(mojServerProperties.getDirectories()
                        .getBaseDirectory()
                        .toFile(), mojServerProperties.getDirectories().getTeamDirectory(),
                team.getName());
        File teamAssignmentDir = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
                mojServerProperties.getDirectories().getTeamDirectory(), team.getName(), file.getAssignment());
        File policy = FileUtils.getFile(mojServerProperties.getDirectories()
                        .getBaseDirectory()
                        .toFile(), mojServerProperties.getDirectories().getLibDirectory(),
                SECURITY_POLICY_FOR_UNIT_TESTS);
        Duration timeout = ad.getTestTimeout() != null ? ad.getTestTimeout() :
                mojServerProperties.getLimits().getTestTimeout();

        if (!policy.exists()) {
            log.error("security policy file not found"); // Exception is swallowed somewhere
            throw new RuntimeException("security policy file not found");
        }

        try {
            boolean isTimeout = false;
            int exitvalue = 0;
            final LengthLimitedOutputCatcher jUnitOutput = new LengthLimitedOutputCatcher(mojServerProperties.getLimits()
                    .getTestOutputLimits());
            final LengthLimitedOutputCatcher jUnitError = new LengthLimitedOutputCatcher(mojServerProperties.getLimits()
                    .getTestOutputLimits());
            try {
                final ProcessExecutor jUnitCommand = new ProcessExecutor().command(mojServerProperties.getLanguages()
                                .getJavaVersion(ad.getJavaVersion())
                                .getRuntime()
                                .toString(), "-cp",
                        makeClasspath(team, file.getAssignment()),
                        "-Djava.security.manager",
                        "-Djava.security.policy=" + policy.getAbsolutePath(),
                        "org.junit.runner.JUnitCore",
                        file.getName());
                log.debug("Executing command {}", jUnitCommand.getCommand().toString().replaceAll(",", "\n"));
                exitvalue = jUnitCommand.directory(teamAssignmentDir)
                        .timeout(timeout.toSeconds(), TimeUnit.SECONDS).redirectOutput(jUnitOutput)
                        .redirectError(jUnitError).execute().getExitValue();
            } catch (TimeoutException e) {
                // process is automatically destroyed
                log.debug("Unit test for {} timed out and got killed", team.getName());
                isTimeout = true;
            } catch (SecurityException se) {
                log.error(se.getMessage(), se);
            }
            if (isTimeout) {
                jUnitOutput.getBuffer()
                        .append('\n')
                        .append(mojServerProperties.getLimits().getTestOutputLimits().getTimeoutMessage());
            }

            final boolean success;
            final String result;
            if (jUnitOutput.length() > 0) {
                stripJUnitPrefix(jUnitOutput.getBuffer());
                // if we still have some output left and exitvalue = 0
                if (jUnitOutput.length() > 0 && exitvalue == 0 && !isTimeout) {
                    success = true;
                } else {
                    success = false;
                }
                result = jUnitOutput.toString();
            } else {
                log.trace(jUnitOutput.toString());
                result = jUnitError.toString();
                success = (exitvalue == 0) && !isTimeout;
            }
            log.info("finished unit test: {}", file.getName());

            testCase = testCaseRepository.save(testCase.toBuilder()
                    .success(success)
                    .timeout(isTimeout)
                    .testOutput(result)
                    .dateTimeEnd(Instant.now())
                    .build());

            testAttempt.getTestCases().add(testCase);

            return TestResult.builder()
                    .testCaseUuid(testCase.getUuid())
                    .success(testCase.isSuccess())
                    .timeout(testCase.isTimeout())
                    .testName(testCase.getName())
                    .testOutput(testCase.getTestOutput())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // TODO this should not be a null result.
        return null;
    }

    private void stripJUnitPrefix(StringBuilder result) {
        final Matcher matcher = JUNIT_PREFIX_P.matcher(result);
        if (matcher.find()) {
            log.trace("stripped '{}'", matcher.group());
            result.delete(0, matcher.end());
            if (result.length() > 0 && result.charAt(0) == '\n') {
                result.deleteCharAt(0);
            }
        } else {
            log.trace("stripped nothing of '{}'", result.subSequence(0, 50));
        }
    }

    private String makeClasspath(Team team, String assignment) {
        File teamAssignmentDir = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
                mojServerProperties.getDirectories().getTeamDirectory(), team.getName(), assignment);
        File classesDir = FileUtils.getFile(teamAssignmentDir, "classes");
        final List<File> classPath = new ArrayList<>();
        classPath.add(classesDir);
        classPath.add(
                FileUtils.getFile(mojServerProperties.getDirectories()
                        .getBaseDirectory()
                        .toFile(), mojServerProperties.getDirectories().getLibDirectory(), "junit-4.12.jar"));
        classPath.add(FileUtils.getFile(mojServerProperties.getDirectories()
                        .getBaseDirectory()
                        .toFile(), mojServerProperties.getDirectories().getLibDirectory(),
                "hamcrest-all-1.3.jar"));
        classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
                mojServerProperties.getDirectories().getLibDirectory(), "asciiart-core-1.1.0.jar"));

        for (File file : classPath) {
            if (!file.exists()) {
                log.error("not found: {}", file.getAbsolutePath());
            } else {
                log.debug("on cp: {}", file.getAbsolutePath());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (File file : classPath) {
            sb.append(file.getAbsolutePath());
            sb.append(System.getProperty("path.separator"));
        }
        return sb.toString();
    }

}
