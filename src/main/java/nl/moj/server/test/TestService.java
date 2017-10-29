package nl.moj.server.test;

import static java.lang.Math.min;

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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import nl.moj.server.DirectoriesConfiguration;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.UnitTestLimitsConfiguration;
import nl.moj.server.competition.Competition;
import nl.moj.server.competition.ScoreService;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.files.AssignmentFile;

@Service
public class TestService {
	private static final Logger log = LoggerFactory.getLogger(TestService.class);

	public static final String SECURITY_POLICY_FOR_UNIT_TESTS = "securityPolicyForUnitTests.policy";
	private static final Pattern JUNIT_PREFIX_P = Pattern.compile("^(JUnit version 4.12)?\\s*\\.?",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private UnitTestLimitsConfiguration limits;

	private Executor testing;

	private DirectoriesConfiguration directories;

	private String javaExecutable;;

	private Competition competition;

	private ScoreService scoreService;

	private FeedbackMessageController feedbackMessageController;

	public TestService(UnitTestLimitsConfiguration limits, @Qualifier("testing") Executor testing,
			DirectoriesConfiguration directories, @Value("${moj.server.javaExecutable}") String javaExecutable,
			Competition competition, ScoreService scoreService, FeedbackMessageController feedbackMessageController) {
		super();
		this.limits = limits;
		this.testing = testing;
		this.directories = directories;
		this.javaExecutable = javaExecutable;
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
					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getTestFiles().stream()
							.filter(f -> tests.contains(f.getName())).collect(Collectors.toList());
					for (AssignmentFile assignmentFile : testFiles) {
						try {
							TestResult tr = unittest(assignmentFile, compileResult);
							tr.setSubmit(false);
							feedbackMessageController.sendTestFeedbackMessage(tr, false, 0);
							result.add(tr);
						} catch (Exception e) {
							final TestResult dummyResult = new TestResult(
									"Server error running tests - contact the Organizer", compileResult.getUser(),
									false, assignmentFile.getFilename());
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
				competition.getCurrentAssignment().addFinishedTeam(compileResult.getUser(),
						compileResult.getScoreAtSubmissionTime());
				if (compileResult.isSuccessful()) {
					try {

						StringBuilder sb = new StringBuilder();
						boolean success = true;
						List<AssignmentFile> testFiles = competition.getCurrentAssignment().getSubmitFiles();
						testFiles.addAll(competition.getCurrentAssignment().getTestFiles());
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
							final TestResult dummyResult = new TestResult(
									"Server error running tests - contact the Organizer", compileResult.getUser(),
									false, e.getMessage());
							feedbackMessageController.sendTestFeedbackMessage(dummyResult, true, 0);
							return dummyResult;
						}
						TestResult result = new TestResult(sb.toString(), compileResult.getUser(), success,
								"Submit Test", compileResult.getScoreAtSubmissionTime());
						Integer score = setFinalAssignmentScore(result, compileResult.getScoreAtSubmissionTime());
						feedbackMessageController.sendTestFeedbackMessage(result, true, score);
						return result;
					} catch (Exception e) {
						e.printStackTrace();
						final TestResult dummyResult = new TestResult(
								"Server error running tests - contact the Organizer", compileResult.getUser(), false,
								"Submit Test");
						feedbackMessageController.sendTestFeedbackMessage(dummyResult, true, 0);
						return dummyResult;
					}
				}
				return null;
			}
		}, testing);

	}

	private Integer setFinalAssignmentScore(TestResult testResult, int scoreAtSubmissionTime) {
		if (testResult.isSuccessful()) {
			feedbackMessageController.sendRefreshToRankingsPage();
			return scoreService.registerScoreAtSubmission(testResult.getUser(), scoreAtSubmissionTime);
		}
		return 0;
	}

	private TestResult unittest(AssignmentFile file, CompileResult compileResult) {

		log.info("running unittest: {}", file.getName());
		File teamdir = FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory(),
				compileResult.getUser());
		File policy = FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory(),
				SECURITY_POLICY_FOR_UNIT_TESTS);
		if (!policy.exists()) {
			log.error("security policy file not found"); // Exception is swallowed somewhere
			throw new RuntimeException("security policy file not found");
		}

		try {
			boolean isRunTerminated = false;
			int exitvalue = 0;
			final LengthLimitedOutputCatcher jUnitOutput = new LengthLimitedOutputCatcher();
			final LengthLimitedOutputCatcher jUnitError = new LengthLimitedOutputCatcher();
			try {
				final ProcessExecutor jUnitCommand = new ProcessExecutor().command(javaExecutable, "-cp",
						makeClasspath(compileResult.getUser()), "-Djava.security.manager",
						"-Djava.security.policy=" + policy.getAbsolutePath(), "org.junit.runner.JUnitCore",
						file.getName());
				log.trace("Executing command {}", jUnitCommand.getCommand().toString().replaceAll(",", "\n"));
				exitvalue = jUnitCommand.directory(teamdir)
						.timeout(limits.getUnitTestTimeoutSeconds(), TimeUnit.SECONDS).redirectOutput(jUnitOutput)
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
				jUnitOutput.getBuffer().append('\n').append(limits.getUnitTestOutput().getTestTimoutTermination());
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
			return new TestResult(result, compileResult.getUser(), success, file.getName());
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
		classPath.add(FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory(), user));
		classPath.add(
				FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory(), "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory(),
				"hamcrest-all-1.3.jar"));
		for (File file : classPath) {
			if (!file.exists()) {
				log.error("not found: {}", file.getAbsolutePath());
			}
		}
		StringBuilder sb = new StringBuilder();
		for (File file : classPath) {
			sb.append(file.getAbsolutePath());
			sb.append(System.getProperty("path.separator"));
		}
		return sb.toString();
	}

	/**
	 * Support class to capture a limited shard of potentially huge output. The
	 * output is limited to a maximum number of lines, a maximum number of chars per
	 * line, and a total maximum number of characters.
	 *
	 * @author hartmut
	 */
	private final class LengthLimitedOutputCatcher extends LogOutputStream {
		private final StringBuilder buffer = new StringBuilder();
		private final int maxSize;
		private final int maxLines;
		private final int maxLineLenght;
		private int lineCount = 0;
		private final String lineTruncatedMessage;
		private final String outputTruncMessage;

		public LengthLimitedOutputCatcher() {
			this.maxSize = limits.getUnitTestOutput().getMaxChars();
			this.maxLines = limits.getUnitTestOutput().getMaxFeedbackLines();
			this.maxLineLenght = limits.getUnitTestOutput().getMaxLineLen();
			this.lineTruncatedMessage = limits.getUnitTestOutput().getLineTruncatedMessage();
			this.outputTruncMessage = limits.getUnitTestOutput().getOutputTruncMessage();
		}

		@Override
		protected void processLine(String line) {
			if (lineCount < maxLines) {
				final int maxAppendFromBufferSize = min(line.length(), maxSize - buffer.length() + 1);
				final int maxAppendFromLineLimit = min(maxAppendFromBufferSize, maxLineLenght);
				if (maxAppendFromLineLimit > 0) {
					final boolean isLineTruncated = maxAppendFromLineLimit < line.length();
					if (isLineTruncated) {
						buffer.append(line.substring(0, maxAppendFromLineLimit)).append(lineTruncatedMessage);
					} else {
						buffer.append(line);
					}
					buffer.append('\n');
				}
			} else if (lineCount == maxLines) {
				buffer.append(outputTruncMessage);
			}
			lineCount++;
		}

		public StringBuilder getBuffer() {
			return buffer;
		}

		@Override
		public String toString() {
			return buffer.toString();
		}

		public int length() {
			return buffer.length();
		}
	}

}
