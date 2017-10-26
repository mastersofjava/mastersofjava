package nl.moj.server.compile;

import java.util.List;

public class CompileResult {

	private String compileResult;

	private List<String> tests;

	private String user;
	
	private boolean successful;
	
	public CompileResult(String compileResult,  List<String> tests, String user, boolean successful) {
		this.compileResult = compileResult;
		this.setTests(tests);
		this.user = user;
		this.successful = successful;
	}

	public String getCompileResult() {
		return compileResult;
	}

	public CompileResult setCompileResult(String compileResult) {
		this.compileResult = compileResult;
		return this;
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

}
