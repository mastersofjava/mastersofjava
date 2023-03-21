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
public class Sources {

    @JsonProperty("base")
    private Path base;

    @JsonProperty("editable")
    private List<Path> editable;

    @JsonProperty("readonly")
    private List<Path> readonly;

    @JsonProperty("hidden")
    private List<Path> hidden;

    public List<Path> getEditable() {
        if (editable == null) {
            return Collections.emptyList();
        }
        return editable;
    }

    public List<Path> getReadonly() {
        if (readonly == null) {
            return Collections.emptyList();
        }
        return readonly;
    }

    public List<Path> getHidden() {
        if (hidden == null) {
            return Collections.emptyList();
        }
        return hidden;
    }
}
