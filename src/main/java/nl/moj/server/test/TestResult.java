package nl.moj.server.test;

import lombok.Data;

@Data
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

}
