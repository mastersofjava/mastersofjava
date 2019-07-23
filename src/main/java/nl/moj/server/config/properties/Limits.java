package nl.moj.server.config.properties;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
public class Limits {

    /**
     * The default compile task timeout.
     */
    private Duration compileTimeout = Duration.ofSeconds(4);

    /**
     * The default test task timeout.
     */
    private Duration testTimeout = Duration.ofSeconds(4);

    @NestedConfigurationProperty
    private OutputLimits testOutputLimits = new OutputLimits();
    @NestedConfigurationProperty
    private OutputLimits compileOutputLimits = new OutputLimits();

    @Data
    public static class OutputLimits {

        private Integer maxFeedbackLines = 1000;
        private Integer maxChars = 10000;
        private Integer maxLineLen = 1000;

        private String lineTruncatedMessage = "...{truncated}";
        private String outputTruncMessage = "...{output truncated}";
        private String timeoutMessage = "...{terminated: time expired}";

    }
}