package nl.moj.server.compile;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class MyRunListener extends RunListener {

	private TestCollector testCollector;
	
    public MyRunListener(TestCollector testCollector) {
    	this.testCollector = testCollector;
	}

	public void testRunStarted(Description description) throws Exception {
		testCollector.addTestResult("Number of tests to execute: " + description.testCount() + "\n");
    }

    public void testRunFinished(Result result) throws Exception {
        testCollector.addTestResult("Number of tests executed: " + result.getRunCount() + "\n");
    }

    public void testStarted(Description description) throws Exception {
        testCollector.addTestResult("Starting: " + description.getMethodName() + "\n");
    }

    public void testFinished(Description description) throws Exception {
        testCollector.addTestResult("Finished: " + description.getMethodName() + "\n");
    }

    public void testFailure(Failure failure) throws Exception {
        testCollector.addTestResult("Failed: " + failure.getDescription().getMethodName() + "\n");
    }

    public void testAssumptionFailure(Failure failure) {
        testCollector.addTestResult("Failed: " + failure.getDescription().getMethodName() + "\n");
    }

    public void testIgnored(Description description) throws Exception {
        testCollector.addTestResult("Ignored: " + description.getMethodName() + "\n");
    }
}
