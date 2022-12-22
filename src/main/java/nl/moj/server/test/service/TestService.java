/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.test.service;

import java.io.File;
import java.nio.file.Path;
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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import nl.moj.server.util.CompletableFutures;
import nl.moj.server.util.LengthLimitedOutputCatcher;

@Service
public class TestService {
	public static final String SECURITY_POLICY_FOR_UNIT_TESTS = "securityPolicyForUnitTests.policy";
	private static final Logger log = LoggerFactory.getLogger(TestService.class);
	private static final Pattern JUNIT_PREFIX_P = Pattern.compile("^(JUnit version 4.12)?\\s*\\.?",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private final MojServerProperties mojServerProperties;

	private final TestCaseRepository testCaseRepository;

	private final TestAttemptRepository testAttemptRepository;

	private final AssignmentStatusRepository assignmentStatusRepository;

	private final TeamService teamService;

	public TestService(MojServerProperties mojServerProperties, CompetitionRuntime competition,
			TestCaseRepository testCaseRepository, TestAttemptRepository testAttemptRepository,
			AssignmentStatusRepository assignmentStatusRepository, TeamService teamService) {
		this.mojServerProperties = mojServerProperties;
		this.testCaseRepository = testCaseRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.assignmentStatusRepository = assignmentStatusRepository;
		this.teamService = teamService;
	}

	public CompletableFuture<TestsOutput> scheduleTests(TestRequest testRequest, List<AssignmentFile> tests,
			Executor executor, ActiveAssignment activeAssignment) {
		TestsInput testsInput = new TestsInput(testRequest, tests, activeAssignment);
		return scheduleTests(executor, testsInput);
	}

	public CompletableFuture<TestsOutput> scheduleTests(Executor executor, TestsInput testsInput) {

		List<CompletableFuture<TestCaseOutput>> testCaseFutures = new ArrayList<CompletableFuture<TestCaseOutput>>();

		Instant dateTimeStart = Instant.now();
		testsInput.getTestCases().forEach(testCaseInput -> testCaseFutures.add(scheduleTest(executor, testCaseInput)));

		// TODO this will be client side, or maybe we want to just one test on the same client?

		return CompletableFutures.allOf(testCaseFutures).thenApply(testCaseOutput -> {
			TestsOutput testsOutput = TestsOutput.builder().testsInput(testsInput).dateTimeStart(dateTimeStart).dateTimeEnd(Instant.now())
					.results(testCaseOutput).build();
			TestAttempt testAttempt = persistTestAttempt(testsOutput);
			testsOutput.setTestAttemptUuid(testAttempt.getUuid());
			return testsOutput;
		});
	}

	private CompletableFuture<TestCaseOutput> scheduleTest(Executor executor, TestCaseInput input) {
		return CompletableFuture.supplyAsync(() -> executeTest(input), executor);
	}

	private TestAttempt persistTestAttempt(TestsOutput testsOutput) {

		log.debug("persisting test attempt for assignment id {}, competitionSessionId {} and teamId {}", testsOutput.getTestsInput().getAssignmentId(),
						testsOutput.getTestsInput().getCompetitionSessionId(), testsOutput.getTestsInput().getTeamId());
		AssignmentStatus assignmentStatus = assignmentStatusRepository
				.findByAssignment_IdAndCompetitionSession_IdAndTeam_Id(testsOutput.getTestsInput().getAssignmentId(),
						testsOutput.getTestsInput().getCompetitionSessionId(), testsOutput.getTestsInput().getTeamId());

		final TestAttempt testAttempt = testAttemptRepository.save(TestAttempt.builder().assignmentStatus(assignmentStatus).dateTimeStart(Instant.now())
				.dateTimeEnd(Instant.now()).uuid(UUID.randomUUID()).build());

		testsOutput.getResults().forEach(output -> {
			TestCase testCase = TestCase.builder().testAttempt(testAttempt).name(output.getInput().getFile().getName())
					.uuid(UUID.randomUUID()).dateTimeStart(output.getDateTimeStart()).success(output.isSuccess())
					.timeout(output.isTimeout()).testOutput(output.getTestOutput()).dateTimeEnd(output.getDateTimeEnd())
					.build();
			
			testCase = testCaseRepository.save(testCase);
			testAttempt.getTestCases().add(testCase);

		});

		return testAttemptRepository.save(testAttempt);

	}

	// TODO this will be client side, or maybe we want to run all tests on the same client?
	private TestCaseOutput executeTest(TestCaseInput input) {

		AssignmentDescriptor ad = input.getAssignmentDescriptor();
		Path teamAssignmentDir = teamService.getTeamAssignmentDirectory(input.getCompetitionSessionUuid(),
				input.getTeamUuid(), input.getAssignmentName());

		Path policy = ad.getAssignmentFiles().getSecurityPolicy();
		if (policy != null) {
			policy = ad.getDirectory().resolve(policy);
		} else {
			policy = mojServerProperties.getDirectories().getBaseDirectory()
					.resolve(mojServerProperties.getDirectories().getLibDirectory())
					.resolve(SECURITY_POLICY_FOR_UNIT_TESTS);
		}

		Duration timeout = ad.getTestTimeout() != null ? ad.getTestTimeout()
				: mojServerProperties.getLimits().getTestTimeout();

		if (!policy.toFile().exists()) {
			log.error(
					"No security policy other than default JVM version installed, refusing to execute tests. Please configure a default security policy.");
			throw new RuntimeException("security policy file not found");
		}
		log.info("starting commandExecutor {} ", teamAssignmentDir);

		try (final LengthLimitedOutputCatcher jUnitOutput = new LengthLimitedOutputCatcher(
				mojServerProperties.getLimits().getTestOutputLimits());
				final LengthLimitedOutputCatcher jUnitError = new LengthLimitedOutputCatcher(
						mojServerProperties.getLimits().getTestOutputLimits())) {
			boolean isTimeout = false;
			int exitvalue = 0;

			Instant dateTimeStart = Instant.now();
			try {
				List<String> commandParts = new ArrayList<>();

				commandParts.add(
						mojServerProperties.getLanguages().getJavaVersion(ad.getJavaVersion()).getRuntime().toString());
				if (ad.getJavaVersion() >= 12) {
					commandParts.add("--enable-preview");
				}
				commandParts.add("-cp");
				commandParts.add(makeClasspath(teamAssignmentDir));
				commandParts.add("-Djava.security.manager");
				commandParts.add("-Djava.security.policy=" + policy.toAbsolutePath());
				commandParts.add("org.junit.runner.JUnitCore");
				commandParts.add(input.getFile().getName());

				final ProcessExecutor commandExecutor = new ProcessExecutor().command(commandParts);
				log.debug("Executing command {}", commandExecutor.getCommand().toString().replaceAll(",", "\n"));
				exitvalue = commandExecutor.directory(teamAssignmentDir.toFile())
						.timeout(timeout.toSeconds(), TimeUnit.SECONDS).redirectOutput(jUnitOutput)
						.redirectError(jUnitError).execute().getExitValue();
			} catch (TimeoutException e) {
				// process is automatically destroyed
				log.debug("Unit test for {} timed out and got killed", input.getTeamName());
				isTimeout = true;
			} catch (SecurityException se) {
				log.debug("Unit test for {} got security error", input.getTeamName());
				log.error(se.getMessage(), se);
			}
			if (isTimeout) {
				jUnitOutput.getBuffer().append('\n')
						.append(mojServerProperties.getLimits().getTestOutputLimits().getTimeoutMessage());
			}

			final boolean success;
			final String result;
			if (jUnitOutput.length() > 0) {
				stripJUnitPrefix(jUnitOutput.getBuffer());
				// if we still have some output left and exitvalue = 0
				success = jUnitOutput.length() > 0 && exitvalue == 0 && !isTimeout;
				result = jUnitOutput.toString();
				if (jUnitOutput.length() == 0) {
					log.info("zero normal junit output, error output: {}-{}", jUnitOutput.toString(),
							jUnitError.toString());
				}
			} else {
				log.info("zero normal junit output, error output: {}", jUnitError.toString());
				result = jUnitError.toString();
				success = (exitvalue == 0) && !isTimeout;
			}
			log.info("finished unit test: {}, exitvalue: {}, outputlength: {}, isTimeout: {} ",
					input.getFile().getName(), exitvalue, result.length(), isTimeout);

			return TestCaseOutput.builder().input(input).dateTimeStart(dateTimeStart).dateTimeEnd(Instant.now())
					.success(success).timeout(isTimeout).testName(input.getFile().getName()).testOutput(result).build();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}

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
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
				mojServerProperties.getDirectories().getLibDirectory(), "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory().toFile(),
				mojServerProperties.getDirectories().getLibDirectory(), "hamcrest-all-1.3.jar"));
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
			sb.append(File.pathSeparator);
		}
		return sb.toString();
	}

}
