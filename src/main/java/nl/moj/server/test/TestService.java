package nl.moj.server.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.MemoryClassLoader;
import nl.moj.server.files.AssignmentFile;

@Service
public class TestService {

	private static final Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	@Qualifier("testing")
	private Executor testing;
	@Value("${moj.server.timeout}")
	private int TIMEOUT;

	@Value("${moj.server.compileBaseDirectory}")
	private String compileBaseDirectory;
	
	@Autowired
	private Competition competition;

	public CompletableFuture<TestResult> test(CompileResult compileResult) {
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				if (compileResult.isSuccessful()) {
					JUnitCore junit = new JUnitCore();
					TestCollector testCollector = new TestCollector();
					MyRunListener myRunListener = new MyRunListener(testCollector);
					junit.addListener(myRunListener);

					List<AssignmentFile> testFiles = competition.getCurrentAssignment().getTestFiles();
					StringBuilder sb = new StringBuilder();
					for (AssignmentFile assignmentFile : testFiles) {
						sb.append(unittest(assignmentFile, compileResult, junit));
					}
					return new TestResult(sb.toString(), compileResult.getUser(),
							!testCollector.isTestFailure());
				} else {
					return new TestResult(compileResult.getCompileResult(), compileResult.getUser(), false);
				}
			}
		}, testing).orTimeout(1, TimeUnit.SECONDS);
	}

	private String unittest(AssignmentFile file, CompileResult compileResult, JUnitCore junit) {
		try {
			log.info("running unittest: {}", file.getName());
			try {
				ProcessBuilder pb = new ProcessBuilder("/usr/lib/jvm/java-9-oracle/bin/java", "-cp", makeClasspath(),
						"org.junit.runner.JUnitCore", file.getName());
				File teamdir = FileUtils.getFile(compileBaseDirectory, compileResult.getUser());
				pb.directory(teamdir);
				//pb.inheritIO();
				
				
				Instant starttijd = Instant.now();
				starttijd = starttijd.plusSeconds(2);
				Process start = pb.start();
				String output = IOUtils.toString(start.getInputStream(), Charset.defaultCharset());
				String erroroutput = IOUtils.toString(start.getErrorStream(), Charset.defaultCharset());
				//CompletableFuture<Process> onExit = start.onExit();
				while (Instant.now().isBefore(starttijd.plusSeconds(2))) {
					System.out.println("waiting");
				}
				log.info("is alive: {} " , start.isAlive());
				if (start.isAlive()) {
					start.destroyForcibly();
					log.info("exitValue " + start.exitValue());
				} else {
					log.info("exitValue " + start.exitValue());
				}
				log.info("finished unittest: {}", file.getName());
				log.info("output {} {}" ,output, erroroutput);
				return output + erroroutput;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SecurityException se) {
			log.error(se.getMessage(), se);
		}
		return null;
	}

	private String makeClasspath() {
		StringBuilder sb = new StringBuilder();
		sb.append(".").append(System.getProperty("path.separator"));
		sb.append("/home/mhayen/Workspaces/workspace-moj/server/lib/junit-4.12.jar")
				.append(System.getProperty("path.separator"));
		sb.append("/home/mhayen/Workspaces/workspace-moj/server/lib/hamcrest-all-1.3.jar");
		return sb.toString();
	}

}
