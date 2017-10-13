package nl.moj.server.test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.MemoryClassLoader;
import nl.moj.server.files.AssignmentFile;

@Service
public class TestService {

	private static final Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private Executor timed;

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
					testFiles.forEach((file) -> unittest(file, compileResult, junit));
					return new TestResult(testCollector.getTestResults(), compileResult.getUser(),
							!testCollector.isTestFailure());
				} else {
					return new TestResult(compileResult.getCompileResult(), compileResult.getUser(), false);
				}
			}
		}, timed);
	}

	private void unittest(AssignmentFile file, CompileResult compileResult, JUnitCore junit) {
		MemoryClassLoader classLoader = new MemoryClassLoader(compileResult.getMemoryMap());
		Class<?> clazz = null;
		try {
			log.info("running unittest: {}", file.getName());
			clazz = classLoader.loadClass(file.getName());
			junit.run(clazz);
		} catch (ClassNotFoundException e) {
			log.error(e.getMessage(), e);
		}
	}

}
