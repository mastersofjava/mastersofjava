package nl.moj.server.test;

public class TestResult {

	private String testResult;
	private String user;
	private boolean successful;

	public TestResult(String testResult, String user, boolean successful) {
		if (testResult.length() > 10000) {
			this.testResult = testResult.substring(0, 10000);	
		} else {
			this.testResult = testResult;
		}
		
		this.user = user;
		this.successful = successful;
	}

	public String getTestResult() {
		return testResult;
	}

	public TestResult setTestResult(String testResult) {
		if (testResult.length() > 10000) {
			this.testResult = testResult.substring(0, 10000);	
		} else {
			this.testResult = testResult;
		}
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
