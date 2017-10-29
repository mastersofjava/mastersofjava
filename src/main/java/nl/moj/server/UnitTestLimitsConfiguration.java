package nl.moj.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "moj.server.limits")
@Configuration
public class UnitTestLimitsConfiguration {

	private Integer unitTestTimeoutSeconds;
	
	private UnitTestOutput unitTestOutput;
	
	public Integer getUnitTestTimeoutSeconds() {
		return unitTestTimeoutSeconds;
	}

	public void setUnitTestTimeoutSeconds(Integer unitTestTimeoutSeconds) {
		this.unitTestTimeoutSeconds = unitTestTimeoutSeconds;
	}

	public UnitTestOutput getUnitTestOutput() {
		return unitTestOutput;
	}

	public void setUnitTestOutput(UnitTestOutput unitTestOutput) {
		this.unitTestOutput = unitTestOutput;
	}

	public static class UnitTestOutput {
	
		private Integer maxFeedbackLines;
		private Integer maxChars;
		private Integer maxLineLen;
		
		private String lineTruncatedMessage;
		private String outputTruncMessage;
		private String testTimoutTermination;
		
		
		public Integer getMaxFeedbackLines() {
			return maxFeedbackLines;
		}
		public void setMaxFeedbackLines(Integer maxFeedbackLines) {
			this.maxFeedbackLines = maxFeedbackLines;
		}
		public Integer getMaxChars() {
			return maxChars;
		}
		public void setMaxChars(Integer maxChars) {
			this.maxChars = maxChars;
		}
		public Integer getMaxLineLen() {
			return maxLineLen;
		}
		public void setMaxLineLen(Integer maxLineLen) {
			this.maxLineLen = maxLineLen;
		}
		public String getLineTruncatedMessage() {
			return lineTruncatedMessage;
		}
		public void setLineTruncatedMessage(String lineTruncatedMessage) {
			this.lineTruncatedMessage = lineTruncatedMessage;
		}
		public String getOutputTruncMessage() {
			return outputTruncMessage;
		}
		public void setOutputTruncMessage(String outputTruncMessage) {
			this.outputTruncMessage = outputTruncMessage;
		}
		public String getTestTimoutTermination() {
			return testTimoutTermination;
		}
		public void setTestTimoutTermination(String testTimoutTermination) {
			this.testTimoutTermination = testTimoutTermination;
		}
	}
}