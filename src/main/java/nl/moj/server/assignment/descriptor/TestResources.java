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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestResources {

    @JsonProperty("base")
    private Path base;

    @JsonProperty("files")
    private List<Path> files;

    @JsonProperty("hidden-files")
    private List<Path> hiddenFiles;

    @JsonProperty("invisible-files")
    private List<Path> invisibleFiles;

    public List<Path> getFiles() {
        if (files == null) {
            return Collections.emptyList();
        }
        return files;
    }

    public List<Path> getHiddenFiles() {
        if (hiddenFiles == null) {
            return Collections.emptyList();
        }
        return hiddenFiles;
    }

    public List<Path> getInvisibleFiles() {
        if (invisibleFiles == null) {
            return Collections.emptyList();
        }
        return invisibleFiles;
    }
}
