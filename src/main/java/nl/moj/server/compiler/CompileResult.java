package nl.moj.server.compiler;

import java.util.ArrayList;
import java.util.List;

public class CompileResult {

	private String result;

	private List<String> tests = new ArrayList<>();

	private String user;
	
	private boolean successful;
	
	private Integer scoreAtSubmissionTime;
	
	public CompileResult(String result,  List<String> tests, String user, boolean successful, Integer scoreAtSubmissionTime) {
		this.setResult(result);
		this.setTests(tests);
		this.user = user;
		this.successful = successful;
		this.scoreAtSubmissionTime = scoreAtSubmissionTime;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public List<String> getTests() {
		return tests;
	}

	public void setTests(List<String> tests) {
		this.tests = tests;
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

	public Integer getScoreAtSubmissionTime() {
		return scoreAtSubmissionTime;
	}

	public void setScoreAtSubmissionTime(Integer scoreAtSubmissionTime) {
		this.scoreAtSubmissionTime = scoreAtSubmissionTime;
	}

}
