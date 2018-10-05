package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "moj.server.limits")
public class Limits {

	private Duration compileTimeout = Duration.ofSeconds(4);
	private Duration testTimeout = Duration.ofSeconds(4);
	
	private OutputLimits testOutputLimits = new OutputLimits();
	private OutputLimits compileOutputLimits = new OutputLimits();

	@Data
	@ConfigurationProperties
	public static class OutputLimits {
	
		private Integer maxFeedbackLines = 1000;
		private Integer maxChars = 10000;
		private Integer maxLineLen = 1000;
		
		private String lineTruncatedMessage = "...{truncated}";
		private String outputTruncMessage = "...{output truncated}";
		private String timeoutMessage = "...{terminated: time expired}";
		
	}
}