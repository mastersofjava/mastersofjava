package nl.moj.server.compile;

import java.util.Map;

public class CompileResult {

	private String compileResult;

	private Map<String, byte[]> memoryMap;

	public CompileResult(String compileResult, Map<String, byte[]> memoryMap) {
		this.compileResult = compileResult;
		this.memoryMap = memoryMap;
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

}
