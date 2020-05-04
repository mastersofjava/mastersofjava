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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentFiles {

    @JsonProperty("assignment")
    private Path assignment;

    @JsonProperty("solution")
    private List<Path> solution = new ArrayList<>();

    @JsonProperty("sources")
    private Sources sources = new Sources();

    @JsonProperty("resources")
    private Resources resources = new Resources();

    @JsonProperty("test-sources")
    private TestSources testSources = new TestSources();

    @JsonProperty("test-resources")
    private TestResources testResources = new TestResources();

    @JsonProperty("security-policy")
    private Path securityPolicy;
}
