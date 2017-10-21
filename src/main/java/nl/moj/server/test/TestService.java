package nl.moj.server.test;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
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

	@Value("${moj.server.compileDirectory}")
	private String compileDirectory;

	@Value("${moj.server.libDirectory}")
	private String libDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

	@Autowired
	private Competition competition;

	public CompletableFuture<TestResult> test(CompileResult compileResult, String test) {
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				if (compileResult.isSuccessful()) {
					boolean testresult = false;
					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getTestFiles().stream()
							.filter(f -> f.getName().equalsIgnoreCase(test)).collect(Collectors.toList());
					StringBuilder sb = new StringBuilder();
					for (AssignmentFile assignmentFile : testFiles) {
						TestResult tr = unittest(assignmentFile, compileResult);
						testresult = tr.isSuccessful();
						sb.append(tr.getTestResult());
					}
					return new TestResult(sb.toString(), compileResult.getUser(), testresult);
				} else {
					return new TestResult(compileResult.getCompileResult(), compileResult.getUser(), false);
				}
			}
		}, testing);//.orTimeout(1, TimeUnit.SECONDS);
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
					return new TestResult(sb.toString(), compileResult.getUser(), true);
				} else {
					return new TestResult(compileResult.getCompileResult(), compileResult.getUser(), false);
				}
			}
		}, testing).orTimeout(1, TimeUnit.SECONDS);

	}

	private TestResult unittest(AssignmentFile file, CompileResult compileResult) {
		try {
			log.info("running unittest: {}", file.getName());
			try {
				ProcessBuilder pb = new ProcessBuilder("/usr/lib/jvm/java-9-oracle/bin/java", "-cp", makeClasspath(compileResult.getUser()),
						"org.junit.runner.JUnitCore", file.getName());
				File teamdir = FileUtils.getFile(basedir, compileDirectory, compileResult.getUser());
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
					List<String> collected = list.stream().filter(line -> !line.trim().startsWith("at")).collect(Collectors.toList());
					output = StringUtils.join(collected, '\n');
				}
				
				
				return new TestResult(output.length() > 0 ? output : erroroutput, compileResult.getUser(),
						start.exitValue() == 0 ? true : false);
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
		StringBuilder sb = new StringBuilder();
		File teamdir = FileUtils.getFile(basedir, compileDirectory, user);
		sb.append(teamdir.getAbsolutePath());
		sb.append(System.getProperty("path.separator"));
		sb.append(basedir +"/" + libDirectory + "/junit-4.12.jar")
				.append(System.getProperty("path.separator"));
		sb.append(basedir +"/" + libDirectory + "/hamcrest-all-1.3.jar");
		System.out.println(sb.toString());
		return sb.toString();
	}

}
