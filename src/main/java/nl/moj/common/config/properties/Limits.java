/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.common.config.properties;

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