package nl.moj.server.test;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TestResult {

	private String result;
	private String user;
	private boolean successful;
	private String testname;
	private boolean submit;
	private Long scoreAtSubmissionTime;

	public TestResult(String testResult, String user, boolean successful, String testname) {
		this.setResult(testResult);
		this.user = user;
		this.successful = successful;
		this.testname = testname;
	}

	public TestResult(String testResult, String user, boolean successful, String testname, Long scoreAtSubmissionTime) {
		this.setResult(testResult);
		this.user = user;
		this.successful = successful;
		this.testname = testname;
		this.scoreAtSubmissionTime = scoreAtSubmissionTime;
	}
}
