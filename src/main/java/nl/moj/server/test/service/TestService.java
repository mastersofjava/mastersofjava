package nl.moj.server.test.service;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.descriptor.ExecutionModel;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import nl.moj.server.util.CompletableFutures;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestService {
    public static final String SECURITY_POLICY_FOR_UNIT_TESTS = "securityPolicyForUnitTests.policy";
    private static final Logger log = LoggerFactory.getLogger(TestService.class);
    private static final Pattern JUNIT_PREFIX_P = Pattern.compile("^(JUnit version 4.12)?\\s*\\.?",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private MojServerProperties mojServerProperties;

    private CompetitionRuntime competition;

    private TestCaseRepository testCaseRepository;

    private TestAttemptRepository testAttemptRepository;

    private AssignmentStatusRepository assignmentStatusRepository;

    private TeamService teamService;

    public TestService(MojServerProperties mojServerProperties, CompetitionRuntime competition,
                       TestCaseRepository testCaseRepository, TestAttemptRepository testAttemptRepository,
                       AssignmentStatusRepository assignmentStatusRepository, TeamService teamService) {
        this.mojServerProperties = mojServerProperties;
        this.competition = competition;
        this.testCaseRepository = testCaseRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.assignmentStatusRepository = assignmentStatusRepository;
        this.teamService = teamService;
    }

    private CompletableFuture<TestResult> scheduleTest(Team team, TestAttempt testAttempt, AssignmentFile test, Executor executor) {
        return CompletableFuture.supplyAsync(() -> executeTest(team, testAttempt, test, competition.getActiveAssignment()), executor);
    }

    public CompletableFuture<TestResults> scheduleTests(Team team, List<AssignmentFile> tests, Executor executor) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment.getAssignment(),
                activeAssignment.getCompetitionSession(), team);
        TestAttempt ta = testAttemptRepository.save(TestAttempt.builder()
                .assignmentStatus(as)
                .dateTimeStart(Instant.now())
                .uuid(UUID.randomUUID())
                .build());

        List<CompletableFuture<TestResult>> testFutures = new ArrayList<>();
        tests.forEach(t -> testFutures.add(scheduleTest(team, ta, t, executor)));

        return CompletableFutures.allOf(testFutures).thenApply( r -> {
            ta.setDateTimeEnd(Instant.now());
            TestAttempt updatedTa = testAttemptRepository.save(ta);
            return TestResults.builder()
                    .dateTimeStart(updatedTa.getDateTimeStart())
                    .dateTimeEnd(updatedTa.getDateTimeEnd())
                    .testAttemptUuid(updatedTa.getUuid())
                    .results(r)
                    .build();
        });
    }

    private TestResult executeTest(Team team, TestAttempt testAttempt, AssignmentFile file, ActiveAssignment activeAssignment) {
        log.info("Running unit test: {}", file.getName());

        TestCase testCase = TestCase.builder()
                .testAttempt(testAttempt)
                .name(file.getName())
                .uuid(UUID.randomUUID())
                .dateTimeStart(Instant.now())
                .build();


        AssignmentDescriptor ad = activeAssignment.getAssignmentDescriptor();
        Path teamAssignmentDir = teamService.getTeamAssignmentDirectory(competition.getCompetitionSession(),team,activeAssignment.getAssignment());

        Path policy = ad.getAssignmentFiles().getSecurityPolicy();
        if( policy != null ) {
            policy = ad.getDirectory().resolve(policy);
        } else {
            policy = mojServerProperties.getDirectories().getBaseDirectory()
                    .resolve(mojServerProperties.getDirectories().getLibDirectory())
                    .resolve(SECURITY_POLICY_FOR_UNIT_TESTS);
        }

        Duration timeout = ad.getTestTimeout() != null ? ad.getTestTimeout() :
                mojServerProperties.getLimits().getTestTimeout();

        if (!policy.toFile().exists()) {
            log.error("No security policy other than default JVM version installed, refusing to execute tests. Please configure a default security policy.");
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
                        makeClasspath(teamAssignmentDir),
                        "-Djava.security.manager",
                        "-Djava.security.policy=" + policy.toAbsolutePath(),
                        "org.junit.runner.JUnitCore",
                        file.getName());
                log.debug("Executing command {}", jUnitCommand.getCommand().toString().replaceAll(",", "\n"));
                exitvalue = jUnitCommand.directory(teamAssignmentDir.toFile())
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
                    .dateTimeStart(testCase.getDateTimeStart())
                    .dateTimeEnd(testCase.getDateTimeEnd())
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

    private String makeClasspath(Path teamAssignmentDir) {
        File classesDir = FileUtils.getFile(teamAssignmentDir.toFile(), "classes");
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
