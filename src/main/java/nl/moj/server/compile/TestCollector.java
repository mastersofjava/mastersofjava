package nl.moj.server.compile;

import org.springframework.stereotype.Component;

@Component
public class TestCollector {

	private StringBuffer testResults = new StringBuffer();
	
	public void addTestResult(String testResult) {
		testResults.append(testResult);
	}
	
	public String getTestResults() {
		return testResults.toString();
	}
}
