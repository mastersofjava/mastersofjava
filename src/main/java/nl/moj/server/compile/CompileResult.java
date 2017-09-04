package nl.moj.server.compile;

import java.util.Map;

public class CompileResult {

	private String compileResult;

	private Map<String, byte[]> memoryMap;

	private String user;
	
	private Boolean successful;
	
	public CompileResult(String compileResult, Map<String, byte[]> memoryMap, String user, Boolean successful) {
		this.compileResult = compileResult;
		this.memoryMap = memoryMap;
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

	public Map<String, byte[]> getMemoryMap() {
		return memoryMap;
	}

	public void setMemoryMap(Map<String, byte[]> memoryMap) {
		this.memoryMap = memoryMap;
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
