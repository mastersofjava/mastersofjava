package nl.moj.server.compile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.springframework.stereotype.Service;

import nl.moj.server.timed.AsyncTimed;

@Service
public class TestService {

	@AsyncTimed
	public CompletableFuture<TestResult> test(CompileResult compileResult) {

		return CompletableFuture.supplyAsync(new Supplier<TestResult>() {

			@Override
			public TestResult get() {
				JUnitCore junit = new JUnitCore();

				MemoryClassLoader classLoader = new MemoryClassLoader(compileResult.getMemoryMap());
				Class<?> clazz = null;
				try {
					clazz = classLoader.loadClass("NogEenTester");
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				TestCollector testCollector = new TestCollector();
				MyRunListener myRunListener = new MyRunListener(testCollector);
				junit.addListener(myRunListener);

				junit.run(clazz);
				return new TestResult(testCollector.getTestResults());
			}
		});
	}
}
