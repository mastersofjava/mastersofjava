package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "moj.server.limits")
public class Limits {

	private Integer unitTestTimeoutSeconds = 4;
	
	private UnitTestOutput unitTestOutput = new UnitTestOutput();

	@ConfigurationProperties
	@Data
	public static class UnitTestOutput {
	
		private Integer maxFeedbackLines = 1000;
		private Integer maxChars = 10000;
		private Integer maxLineLen = 1000;
		
		private String lineTruncatedMessage = "...{truncated}";
		private String outputTruncMessage = "...{output truncated}";
		private String testTimeoutTermination = "...{terminated: test time expired}";
		
	}
}