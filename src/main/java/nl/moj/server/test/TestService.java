package nl.moj.server.test;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.moj.server.FeedbackController;
import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.files.AssignmentFile;

@Service
public class TestService {

	private static final int MAX_FEEDBACK_SIZE = 10000;

	private static final Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	@Qualifier("testing")
	private Executor testing;
	@Value("${moj.server.timeout}")
	private int timeout;

	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.libDirectory}")
	private String libDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

	@Value("${moj.server.javaExecutable}")
	private String javaExecutable;

	@Autowired
	private Competition competition;

	@Autowired
	private FeedbackController feedback;

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
						TestResult tr = unittest(assignmentFile, compileResult);
						tr.setSubmit(false);
						feedback.sendTestFeedbackMessage(tr, false);
					}
					return result;
				} else {
					return new ArrayList<>();
				}
			}
		}, testing);
	}

	public CompletableFuture<TestResult> testSubmit(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				competition.getCurrentAssignment().addFinishedTeam(compileResult.getUser());
				if (compileResult.isSuccessful()) {
					TestResult tr = null;
					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getSubmitFiles();
					// there should be only 1;
					tr = unittest(testFiles.get(0), compileResult);
					tr.setSubmit(true);
					feedback.sendTestFeedbackMessage(tr, true);
					return tr;
				}
				return null;
			}
		}, testing);

	}

	private TestResult unittest(AssignmentFile file, CompileResult compileResult) {
		try {
			log.info("running unittest: {}", file.getName());
			try {
				ProcessBuilder pb = new ProcessBuilder(javaExecutable, "-cp", makeClasspath(compileResult.getUser()),
						"org.junit.runner.JUnitCore", file.getName());
				File teamdir = FileUtils.getFile(basedir, teamDirectory, compileResult.getUser());
				pb.directory(teamdir);
				for (String s : pb.command()) {
					log.debug(s);
				}
				Instant starttijd = Instant.now();
				starttijd = starttijd.plusSeconds(2);
				Process start = pb.start();
				String output = IOUtils.toString(start.getInputStream(), Charset.defaultCharset());
				String erroroutput = IOUtils.toString(start.getErrorStream(), Charset.defaultCharset());
				log.debug("is alive: {} ", start.isAlive());
				while (start.isAlive() && Instant.now().isBefore(starttijd.plusSeconds(2))) {
					Thread.sleep(100);
				}
				if (start.isAlive()) {
					log.debug("still alive, killing: {} ", start.isAlive());
					start.destroyForcibly();
				}
				log.debug("exitValue {}", start.exitValue());
				int exitvalue = start.exitValue();
				boolean success = false;
				String result = null;
				if (output == null) {
					return new TestResult(result, compileResult.getUser(), success, file.getName());
				}
				if (output.length() > MAX_FEEDBACK_SIZE) {
					result = output.substring(0, MAX_FEEDBACK_SIZE);
				} else {
					result = output;
				}
				if (result.length() > 0) {
					result = result.substring("JUnit version 4.12\n".length());
					result = result.substring(".".length());
					// if we still have some output left and exitvalue = 0
					if (result.length() > 0 && exitvalue == 0 ? true : false) {
						success = true;
						// result = filteroutput(output);
					}
				} else {
					System.out.print(result);
					result = erroroutput;
					success = exitvalue == 0 ? true : false;
				}
				log.debug("success {}", success);
				log.info("finished unittest: {}", file.getName());
				return new TestResult(result, compileResult.getUser(), success, file.getName());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} catch (SecurityException se) {
			log.error(se.getMessage(), se);
		}
		return null;
	}

	private String filteroutput(String output) {
		String[] split = output.split("\n");
		List<String> list = Arrays.asList(split);
		List<String> collected = list.stream() //
				// .filter(line -> !line.trim().startsWith("at")) //
				.filter(line -> !line.trim().startsWith(".")) //
				.collect(Collectors.toList());
		return StringUtils.join(collected, '\n');
	}

	private String makeClasspath(String user) {
		final List<File> classPath = new ArrayList<>();
		classPath.add(FileUtils.getFile(basedir, teamDirectory, user));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "hamcrest-all-1.3.jar"));
		for (File file : classPath) {
			if (!file.exists()) {
				System.out.println("not found: " + file.getAbsolutePath());
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
