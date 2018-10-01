package nl.moj.server.test;

import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.teams.model.Team;
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

	public TestService(MojServerProperties mojServerProperties, @Qualifier("testing") Executor executor) {
		this.mojServerProperties = mojServerProperties;
		this.executor = executor;
	}

	public CompletableFuture<List<TestResult>> runTests(Team team, List<AssignmentFile> tests) {
		return CompletableFuture.supplyAsync(() -> {
			List<TestResult> results = new ArrayList<>();
			for (AssignmentFile test : tests) {
				try {
					results.add(runTest(team, test));
				} catch (Exception e) {
					results.add(TestResult.builder()
							.message("Server error running tests - contact the Organizer")
							.team(team).successful(false).testName(test.getName()).build());

				}
			}
			return results;
		}, executor);
	}

	private TestResult runTest(Team team, AssignmentFile file) {

		log.info("Running unit test: {}", file.getName());
		File teamdir = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getTeamDirectory(),
				team.getName());
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
						makeClasspath(team, file.getAssignment()),
						"-Djava.security.manager",
						"-Djava.security.policy=" + policy.getAbsolutePath(),
						"org.junit.runner.JUnitCore",
						file.getName());
				log.debug("Executing command {}", jUnitCommand.getCommand().toString().replaceAll(",", "\n"));
				exitvalue = jUnitCommand.directory(teamdir)
						.timeout(mojServerProperties.getLimits().getUnitTestTimeoutSeconds(), TimeUnit.SECONDS).redirectOutput(jUnitOutput)
						.redirectError(jUnitError).execute().getExitValue();
			} catch (TimeoutException e) {
				// process is automatically destroyed
				log.debug("Unit test for {} timed out and got killed", team.getName());
				isRunTerminated = true;
			} catch (SecurityException se) {
				log.error(se.getMessage(), se);
			}
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
			log.info("finished unit test: {}", file.getName());
			return TestResult.builder().message(result).team(team).successful(success)
					.testName(file.getName()).build();
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

	private String makeClasspath(Team team, String assignment) {
		File teamAssignmentDir = FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(),
				mojServerProperties.getDirectories().getTeamDirectory(), team.getName(), assignment );
		File classesDir = FileUtils.getFile(teamAssignmentDir, "classes");
		final List<File> classPath = new ArrayList<>();
		classPath.add(classesDir);
		classPath.add(
				FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getLibDirectory(), "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(), mojServerProperties.getDirectories().getLibDirectory(),
				"hamcrest-all-1.3.jar"));
		classPath.add(FileUtils.getFile(mojServerProperties.getDirectories().getBaseDirectory(),
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
