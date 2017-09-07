package nl.moj.server.test;

import org.springframework.stereotype.Component;

@Component
public class TestCollector {

	private StringBuffer testResults = new StringBuffer();
	
	private boolean testFailure;
	
	public void addTestResult(String testResult) {
		testResults.append(testResult);
	}
	
	public String getTestResults() {
		return testResults.toString();
	}

	public boolean isTestFailure() {
		return testFailure;
	}

	public void setTestFailure(boolean testFailure) {
		this.testFailure = testFailure;
	}
}
