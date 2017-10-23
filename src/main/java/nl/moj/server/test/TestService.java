package nl.moj.server.test;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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
import nl.moj.server.SubmitController.FeedbackMessage;
import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.files.AssignmentFile;

@Service
public class TestService {

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

	@Autowired
	private Competition competition;

	@Autowired
	private FeedbackController feedback;
	
	

	public CompletableFuture<TestResult> test(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				if (compileResult.isSuccessful()) {
					boolean testresult = false;
					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getTestFiles();
					StringBuilder sb = new StringBuilder();
					for (AssignmentFile assignmentFile : testFiles) {
						TestResult tr = unittest(assignmentFile, compileResult);
						testresult = tr.isSuccessful();
						sb.append(tr.getTestResult());
					}
					return new TestResult(sb.toString(), compileResult.getUser(), testresult, "");
				} else {
					return new TestResult(compileResult.getCompileResult(), compileResult.getUser(), false ,"");
				}
			}
		}, testing);// .orTimeout(1, TimeUnit.SECONDS);
	}

	public CompletableFuture<List<TestResult>> testAll(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<List<TestResult>>() {
			@Override
			public List<TestResult> get() {
				if (compileResult.isSuccessful()) {
					List<TestResult> result = new ArrayList<>();
					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getTestFiles();
					for (AssignmentFile assignmentFile : testFiles) {
						TestResult tr = unittest(assignmentFile, compileResult);
						result.add(tr);
						feedback.sendFeedbackMessage(tr);

						
					}
					return result;
				} else {
					return new ArrayList<>();
				}
			}
		}, testing);// .orTimeout(1, TimeUnit.SECONDS);
	}
	
	public CompletableFuture<TestResult> testSubmit(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				if (compileResult.isSuccessful()) {

					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getTestAndSubmitFiles();
					StringBuilder sb = new StringBuilder();
					for (AssignmentFile assignmentFile : testFiles) {
						sb.append(unittest(assignmentFile, compileResult));
					}
					return new TestResult(sb.toString(), compileResult.getUser(), true, "submit");
				} else {
					return new TestResult(compileResult.getCompileResult(), compileResult.getUser(), false, "submit");
				}
			}
		}, testing).orTimeout(1, TimeUnit.SECONDS);

	}

	private TestResult unittest(AssignmentFile file, CompileResult compileResult) {
		try {
			log.info("running unittest: {}", file.getName());
			try {
				ProcessBuilder pb = new ProcessBuilder("/usr/lib/jvm/java-9-oracle/bin/java", "-cp",
						makeClasspath(compileResult.getUser()), "org.junit.runner.JUnitCore", file.getName());
				File teamdir = FileUtils.getFile(basedir, teamDirectory, compileResult.getUser());
				pb.directory(teamdir);
				for (String s : pb.command()) {
					System.out.println(s);
				}
				// pb.inheritIO();

				Instant starttijd = Instant.now();
				starttijd = starttijd.plusSeconds(2);
				Process start = pb.start();
				String output = IOUtils.toString(start.getInputStream(), Charset.defaultCharset());
				String erroroutput = IOUtils.toString(start.getErrorStream(), Charset.defaultCharset());
				// CompletableFuture<Process> onExit = start.onExit();
				while (start.isAlive() && Instant.now().isBefore(starttijd.plusSeconds(2))) {
					Thread.sleep(1000);
				}
				log.info("is alive: {} ", start.isAlive());
				if (start.isAlive()) {
					start.destroyForcibly();
					log.info("exitValue " + start.exitValue());
				} else {
					log.info("exitValue " + start.exitValue());
				}
				log.info("finished unittest: {}", file.getName());
				// log.info("output {} {}", output, erroroutput);
				if (output != null && output.length() > 0 && output.contains("JUnit version 4.12")) {
					output = output.substring("JUnit version 4.12".length());
					String[] split = output.split("\n");
					List<String> list = Arrays.asList(split);
					List<String> collected = list.stream()
							.filter(line -> !line.trim().startsWith("at"))
							.filter(line -> !line.trim().startsWith("."))
							.collect(Collectors.toList());
					output = StringUtils.join(collected, '\n');
				}

				return new TestResult(output.length() > 0 ? output : erroroutput, compileResult.getUser(),
						start.exitValue() == 0 ? true : false, file.getName());
				// return output + erroroutput;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SecurityException se) {
			log.error(se.getMessage(), se);
		}
		return null;
	}

	private String makeClasspath(String user) {
		final List<File> classPath = new ArrayList<>();
		classPath.add(FileUtils.getFile(basedir, teamDirectory, user));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "junit-4.12.jar"));
		classPath.add(FileUtils.getFile(basedir, libDirectory, "hamcrest-all-1.3.jar"));
		for (File file : classPath) {
			if (!file.exists()) {
				System.out.println("not found: " + file.getAbsolutePath());
			} else {
				System.out.println("found: " + file.getAbsolutePath());
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
