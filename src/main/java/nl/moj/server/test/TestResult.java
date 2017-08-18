package nl.moj.server.test;

public class TestResult {

	private String testResult;
	private String user;
	
	public TestResult(String testResult, String user) {
		this.testResult = testResult;
		this.user = user;
	}
	public String getTestResult() {
		return testResult;
	}

	public TestResult setTestResult(String testResult) {
		this.testResult = testResult;
		return this;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	
}
