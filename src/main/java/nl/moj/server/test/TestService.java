package nl.moj.server.test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.tascalate.concurrent.CompletableTask;
import net.tascalate.concurrent.Promise;
import nl.moj.server.AssignmentService;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.MemoryClassLoader;
import nl.moj.server.files.AssignmentFile;

@Service
public class TestService {

	private static final Logger log = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private Executor timed;
	
	@Autowired
	private AssignmentService assignmentService;

	public Promise<TestResult> test(CompileResult compileResult) {
		return CompletableTask.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				if (compileResult.isSuccessful()) {
					JUnitCore junit = new JUnitCore();
					TestCollector testCollector = new TestCollector();
					MyRunListener myRunListener = new MyRunListener(testCollector);
					junit.addListener(myRunListener);

					List<AssignmentFile> testFiles = assignmentService.getTestFiles();
					testFiles.forEach((file) -> unittest(file, compileResult, junit));
					return new TestResult(testCollector.getTestResults(),compileResult.getUser());					
				} else {
					return new TestResult(compileResult.getCompileResult(),compileResult.getUser());
				}
			}
		}, timed);
	}
	public Promise<TestResult> submit(CompileResult compileResult) {
		return CompletableTask.supplyAsync(new Supplier<TestResult>() {
			@Override
			public TestResult get() {
				if (compileResult.isSuccessful()) {
					JUnitCore junit = new JUnitCore();
					TestCollector testCollector = new TestCollector();
					MyRunListener myRunListener = new MyRunListener(testCollector);
					junit.addListener(myRunListener);

					List<AssignmentFile> testFiles = assignmentService.getTestAndSubmitFiles();
					testFiles.forEach((file) -> unittest(file, compileResult, junit));
					return new TestResult(testCollector.getTestResults(),compileResult.getUser());					
				} else {
					return new TestResult(compileResult.getCompileResult(),compileResult.getUser());
				}
			}
		}, timed);
	}
	

	public Supplier<TestResult> tester(CompileResult compileResult) {
		Supplier<TestResult> supplier = () -> {
			JUnitCore junit = new JUnitCore();
			TestCollector testCollector = new TestCollector();
			MyRunListener myRunListener = new MyRunListener(testCollector);
			junit.addListener(myRunListener);

			List<AssignmentFile> testFiles = assignmentService.getTestFiles();
			testFiles.forEach((file) -> unittest(file, compileResult, junit));
			return new TestResult(testCollector.getTestResults(),compileResult.getUser());
		};
		return supplier;
	}	
	
	public Function<CompileResult,TestResult> tester2(CompileResult compileResult) {
		Function<CompileResult,TestResult> supplier = x -> {
			JUnitCore junit = new JUnitCore();
			TestCollector testCollector = new TestCollector();
			MyRunListener myRunListener = new MyRunListener(testCollector);
			junit.addListener(myRunListener);

			List<AssignmentFile> testFiles = assignmentService.getTestFiles();
			testFiles.forEach((file) -> unittest(file, compileResult, junit));
			return new TestResult(testCollector.getTestResults(),compileResult.getUser());
		};
		return supplier;
	}
	
	public Function<CompileResult,TestResult> tester3() {
		TestCollector testCollector = new TestCollector();
		Function<CompileResult,TestResult> supplier = compileResult -> {
			JUnitCore junit = new JUnitCore();
			MyRunListener myRunListener = new MyRunListener(testCollector);
			junit.addListener(myRunListener);

			List<AssignmentFile> testFiles = assignmentService.getTestFiles();
			testFiles.forEach((file) -> unittest(file, compileResult, junit));
			return new TestResult(testCollector.getTestResults(),compileResult.getUser());
		};
		return supplier;
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
