package nl.moj.server.test;

import nl.moj.server.FeedbackMessageController;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.compiler.CompileResult;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.ScoreService;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.util.LengthLimitedOutputCatcher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.min;

@Service
public class TestService {
	public static final String SECURITY_POLICY_FOR_UNIT_TESTS = "securityPolicyForUnitTests.policy";
	private static final Logger log = LoggerFactory.getLogger(TestService.class);
	private static final Pattern JUNIT_PREFIX_P = Pattern.compile("^(JUnit version 4.12)?\\s*\\.?",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private Executor testing;

	private MojServerProperties mojServerProperties;

	private CompetitionRuntime competition;

	private ScoreService scoreService;

	private FeedbackMessageController feedbackMessageController;

	public TestService(MojServerProperties mojServerProperties, @Qualifier("testing") Executor testing,
					   CompetitionRuntime competition, ScoreService scoreService, FeedbackMessageController feedbackMessageController) {
		this.mojServerProperties = mojServerProperties;
		this.testing = testing;
		this.competition = competition;
		this.scoreService = scoreService;
		this.feedbackMessageController = feedbackMessageController;
	}

	/**
	 * Tests all normal unit tests. The Submit test will NOT be tested.
	 *
	 * @param compileResult
	 * @return
	 */
	public CompletableFuture<List<TestResult>> testAll(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<List<TestResult>>() {
			@Override
			public List<TestResult> get() {
				if (compileResult.isSuccessful()) {
					List<TestResult> result = new ArrayList<>();
					List<String> tests = compileResult.getTests();
					List<AssignmentFile> testFiles = competition.getAssignmentState().getAssignmentFiles().stream()
							.filter(f -> tests.contains(f.getName())).collect(Collectors.toList());
					for (AssignmentFile assignmentFile : testFiles) {
						try {
							TestResult tr = unittest(assignmentFile, compileResult);
							tr.setSubmit(false);
							feedbackMessageController.sendTestFeedbackMessage(tr, false, 0);
							result.add(tr);
						} catch (Exception e) {
							final TestResult dummyResult = TestResult.builder()
									.result("Server error running tests - contact the Organizer")
									.user(compileResult.getUser()).successful(false)
									.testname(assignmentFile.getFilename()).build();
							feedbackMessageController.sendTestFeedbackMessage(dummyResult, false, 0);
							result.add(dummyResult);
						}
					}
					return result;
				} else {
					return new ArrayList<>();
				}
			}
		}, testing);
	}

	/**
	 * Test the solution provided by the team against the Submit test and assignment
	 * tests. All tests have to succeed.
	 *
	 * @param compileResult
	 * @return the combined TestResult
	 */
	public CompletableFuture<TestResult> testSubmit(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				AssignmentState state = competition.getAssignmentState();
				Assignment assignment = competition.getCurrentAssignment().getAssignment();
				Long finalScore = 0L;
				final Long submissionTime = compileResult.getScoreAtSubmissionTime(); // Identical to score at
				// submission time
				if (compileResult.isSuccessful()) {
					try {

						StringBuilder sb = new StringBuilder();
						boolean success = true;
						List<AssignmentFile> testFiles = state.getSubmitFiles();
						testFiles.addAll(state.getTestFiles());
						testFiles.forEach(f -> log.trace(f.getName()));
						try {
							for (AssignmentFile assignmentFile : testFiles) {
								TestResult tr = unittest(assignmentFile, compileResult);
								sb.append(tr.getResult());
								if (success) {
									success = tr.isSuccessful();
									log.debug("set success {}", tr.isSuccessful());
								}
							}
						} catch (Exception e) {
							final TestResult dummyResult = TestResult.builder()
									.result("Server error running tests - contact the Organizer")
									.user(compileResult.getUser()).successful(false).testname(e.getMessage()).build();
							feedbackMessageController.sendTestFeedbackMessage(dummyResult, true, 0);
							return dummyResult;
						}

						final TestResult result = TestResult.builder().result(sb.toString())
								.user(compileResult.getUser()).successful(success).testname("Submit Test")
								.scoreAtSubmissionTime(submissionTime).build();

						// TODO we should not register this here, this needs to be done in the score
						// service probably.
						finalScore = setFinalAssignmentScore(result, assignment, submissionTime);
						// TODO fix possible precision loss
						feedbackMessageController.sendTestFeedbackMessage(result, true, finalScore.intValue());
						return result;
					} catch (Exception e) {
						log.error("Exception Running tests", e);
						final TestResult dummyResult = TestResult.builder()
								.result("Server error running tests - contact the Organizer")
								.user(compileResult.getUser()).successful(false).testname("Submit Test")
								.scoreAtSubmissionTime(0L).build();
						feedbackMessageController.sendTestFeedbackMessage(dummyResult, true, 0);
						return dummyResult;
					} finally {
						// TODO we should not register this here, this needs to be done in the score
						// service probably.
						competition.registerFinishedTeam(compileResult.getUser(), submissionTime, finalScore);
					}

				} else { // Compile failed
					final TestResult compileFailedResult = TestResult.builder()
							.result("Submit Test - Compilation failed->test failed").user(compileResult.getUser())
							.successful(false).testname("Submit Test").scoreAtSubmissionTime(0L).build();

					feedbackMessageController.sendTestFeedbackMessage(compileFailedResult, true, -1);

					// TODO we should not register this here, this needs to be done in the score
					// service probably.
					setFinalAssignmentScore(compileFailedResult, assignment, 0L);
					competition.registerFinishedTeam(compileResult.getUser(), submissionTime, 0L);
					return compileFailedResult;
				}
			}
		}, testing);

	}

	private Long setFinalAssignmentScore(TestResult testResult, Assignment assignment, Long scoreAtSubmissionTime) {
		Long score = scoreService.registerScoreAtSubmission(testResult.getUser(), assignment,
				testResult.isSuccessful() ? scoreAtSubmissionTime : 0L);
		feedbackMessageController.sendRefreshToRankingsPage();
		return score;
	}

	private TestResult unittest(AssignmentFile file, CompileResult compileResult) {

		log.info("running unittest: {}", file.getName());
		File teamdir = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getTeamDirectory(),
				compileResult.getUser());
		File policy = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getLibDirectory(),
				SECURITY_POLICY_FOR_UNIT_TESTS);
		if (!policy.exists()) {
			log.error("security policy file not found"); // Exception is swallowed somewhere
			throw new RuntimeException("security policy file not found");
		}

		try {
			boolean isRunTerminated = false;
			int exitvalue = 0;
			final LengthLimitedOutputCatcher jUnitOutput = new LengthLimitedOutputCatcher(mojServerProperties);
			final LengthLimitedOutputCatcher jUnitError = new LengthLimitedOutputCatcher(mojServerProperties);
			try {
				final ProcessExecutor jUnitCommand = new ProcessExecutor().command(mojServerProperties.getLanguages().getJavaVersion().getRuntime().toString(), "-cp",
						makeClasspath(compileResult.getUser()), "-Djava.security.manager",
						"-Djava.security.policy=" + policy.getAbsolutePath(), "org.junit.runner.JUnitCore",
						file.getName());
				log.debug("Executing command {}", jUnitCommand.getCommand().toString().replaceAll(",", "\n"));
				exitvalue = jUnitCommand.directory(teamdir)
						.timeout(mojServerProperties.getLimits().getUnitTestTimeoutSeconds(), TimeUnit.SECONDS).redirectOutput(jUnitOutput)
						.redirectError(jUnitError).execute().getExitValue();
			} catch (TimeoutException e) {
				// process is automatically destroyed
				log.debug("Unit test for {} timed out and got killed", compileResult.getUser());
				isRunTerminated = true;
			} catch (SecurityException se) {
				log.error(se.getMessage(), se);
			}
			log.debug("exitValue {}", exitvalue);
			if (isRunTerminated) {
				jUnitOutput.getBuffer().append('\n').append(mojServerProperties.getLimits().getUnitTestOutput().getTestTimeoutTermination());
			}

			final boolean success;
			final String result;
			if (jUnitOutput.length() > 0) {
				stripJUnitPrefix(jUnitOutput.getBuffer());
				// if we still have some output left and exitvalue = 0
				if (jUnitOutput.length() > 0 && exitvalue == 0 && !isRunTerminated) {
					success = true;
				} else {
					success = false;
				}
				result = jUnitOutput.toString();
			} else {
				log.trace(jUnitOutput.toString());
				result = jUnitError.toString();
				success = (exitvalue == 0) && !isRunTerminated;
			}

			log.debug("success {}", success);
			log.info("finished unittest: {}", file.getName());
			return TestResult.builder().result(result).user(compileResult.getUser()).successful(success)
					.testname(file.getName()).build();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
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

	private String makeClasspath(String user) {
		final List<File> classPath = new ArrayList<>();
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getTeamDirectory(), user));
		classPath.add(
				FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getLibDirectory(), "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getLibDirectory(),
				"hamcrest-all-1.3.jar"));
		
		if (mojServerProperties.getDirectories().getResourceDirectory()==null) {
			log.warn("no moj.server.directories.resourceDirectory configured in application.yaml, no resources can be used by assignments!");
		} else {
			classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getResourceDirectory()));
		}
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
