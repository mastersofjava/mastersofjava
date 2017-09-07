package nl.moj.server.test;

public class TestResult {

	private String testResult;
	private String user;
	private boolean successful;

	public TestResult(String testResult, String user, boolean successful) {
		this.testResult = testResult;
		this.user = user;
		this.successful = successful;
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

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

}
