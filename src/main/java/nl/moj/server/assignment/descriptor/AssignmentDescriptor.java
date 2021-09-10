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
package nl.moj.server.assignment.descriptor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentDescriptor {

    private Path directory;

    @JsonProperty("name")
    private String name;
    @JsonProperty("display-name")
    private String displayName;
    @JsonProperty("author")
    private Author author;
    @JsonProperty("image")
    private Path image;
    @JsonProperty("sponsor-image")
    private Path sponsorImage;
    @JsonProperty("labels")
    private List<String> labels;
    @JsonProperty("difficulty")
    private Integer difficulty;
    @JsonProperty("java-version")
    private Integer javaVersion;
    @JsonProperty("duration")
    private Duration duration;
    @JsonProperty("submit-timeout")
    private Duration submitTimeout;
    @JsonProperty("test-timeout")
    private Duration testTimeout;
    @JsonProperty("compile-timeout")
    private Duration compileTimeout;
    @JsonProperty("execution-model")
    private ExecutionModel executionModel;

    @JsonProperty("scoring-rules")
    private ScoringRules scoringRules;
    @JsonProperty("assignment-files")
    private AssignmentFiles assignmentFiles;

    public List<String> readScoreLables() {
        List<String> scoreLabels = new ArrayList<>();
        for (String label :this.getLabels()) {
            if (label.startsWith("test")) {
                scoreLabels.add(label);
            }
        }
        return scoreLabels;
    }
}
