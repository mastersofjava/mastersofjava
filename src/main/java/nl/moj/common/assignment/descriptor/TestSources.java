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
package nl.moj.common.assignment.descriptor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSources {

    @JsonProperty("base")
    private Path base;

    @JsonProperty("tests")
    private List<Path> tests;

    @JsonProperty("hidden-tests")
    private List<Path> hiddenTests;

    @JsonProperty("invisible-tests")
    private List<Path> invisibleTests;

    public List<Path> getTests() {
        if (tests == null) {
            return Collections.emptyList();
        }
        return tests;
    }

    public List<Path> getHiddenTests() {
        if (hiddenTests == null) {
            return Collections.emptyList();
        }
        return hiddenTests;
    }

    public List<Path> getInvisibleTests() {
        if (invisibleTests == null) {
            return Collections.emptyList();
        }
        return invisibleTests;
    }

    public int getTotalTestCount() {
        return getTests().size() + getHiddenTests().size() + getInvisibleTests().size();
    }
}
