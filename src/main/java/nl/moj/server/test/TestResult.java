package nl.moj.server.test;

public class TestResult {

	private String result;
	private String user;
	private boolean successful;
	private String testname;
	private boolean submit;
	private Integer scoreAtSubmissionTime;

	public TestResult(String testResult, String user, boolean successful, String testname) {
		this.setResult(testResult);
		this.user = user;
		this.successful = successful;
		this.testname = testname;
	}

	public TestResult(String testResult, String user, boolean successful, String testname, Integer scoreAtSubmissionTime) {
		this.setResult(testResult);
		this.user = user;
		this.successful = successful;
		this.testname = testname;
		this.scoreAtSubmissionTime = scoreAtSubmissionTime;
	}
	
	public TestResult() {
		// default
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
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

	public Integer getScoreAtSubmissionTime() {
		return scoreAtSubmissionTime;
	}

	public void setScoreAtSubmissionTime(Integer scoreAtSubmissionTime) {
		this.scoreAtSubmissionTime = scoreAtSubmissionTime;
	}
}
