package nl.moj.server.test;

public class TestResult {

	private String testResult;
	private String user;
	private boolean successful;
	private String testname;
	private boolean submit;

	public TestResult(String testResult, String user, boolean successful, String testname) {
		this.testResult = testResult;
		this.user = user;
		this.successful = successful;
		this.testname = testname;
	}

	public TestResult() {
		// TODO Auto-generated constructor stub
	}

	public String getTestResult() {
		return testResult;
	}

	public void setTestResult(String testResult) {
		this.testResult = testResult;
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

	public String getTestname() {
		return testname;
	}

	public void setTestname(String testname) {
		this.testname = testname;
	}

	public boolean isSubmit() {
		return submit;
	}

	public void setSubmit(boolean submit) {
		this.submit = submit;
	}
}
