package nl.moj.server.test;

public class TestResult {

	private String testResult;

	public TestResult(String testResult) {
		this.testResult = testResult;
	}
	public String getTestResult() {
		return testResult;
	}

	public TestResult setTestResult(String testResult) {
		this.testResult = testResult;
		return this;
	}
	
}
