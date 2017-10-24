package nl.moj.server.compile;

import java.util.List;
import java.util.Map;

public class CompileResult {

	private String compileResult;

	private List<String> tests;

	private String user;
	
	private Boolean successful;
	
	public CompileResult(String compileResult,  List<String> tests, String user, Boolean successful) {
		this.compileResult = compileResult;
		this.setTests(tests);
		this.user = user;
		this.setSuccessful(successful);
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

	public Boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(Boolean successful) {
		this.successful = successful;
	}

}
