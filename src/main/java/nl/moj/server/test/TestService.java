package nl.moj.server.test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.junit.runner.JUnitCore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.AssignmentService;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.MemoryClassLoader;
import nl.moj.server.files.AssignmentFile;
import nl.moj.server.timed.AsyncTimed;

@Service
public class TestService {

	@Autowired
	private Executor timed;
	
	@Autowired
	private AssignmentService assignmentService;

	private CompileResult compileResult;

	private JUnitCore junit = new JUnitCore();
	
	private TestCollector testCollector = new TestCollector();
	
	@AsyncTimed
	public CompletableFuture<TestResult> test(CompileResult compileResult) {
		this.compileResult = compileResult;
		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {

			@Override
			public TestResult get() {
				
				MyRunListener myRunListener = new MyRunListener(testCollector);
				junit.addListener(myRunListener);

				List<AssignmentFile> testFiles = assignmentService.getTestFiles();
				testFiles.forEach(file -> doit(file)						);
				return new TestResult(testCollector.getTestResults());
			}
		}, timed);
	}
	
	private void doit(AssignmentFile file) {
		MemoryClassLoader classLoader = new MemoryClassLoader(compileResult.getMemoryMap());
		Class<?> clazz = null;
		try {
			clazz = classLoader.loadClass(file.getName());
			junit.run(clazz);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
	}
}
