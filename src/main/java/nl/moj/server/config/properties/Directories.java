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
package nl.moj.server.config.properties;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Data;

@Data
public class Directories {

    @NotNull
    private Path baseDirectory;
    private String sessionDirectory = "sessions";
    private String teamDirectory = "teams";
    private String libDirectory = "lib";
    private String soundDirectory = "sounds";
    private String javadocDirectory = "javadoc";

    public Path getBaseDirectory() {
        if (baseDirectory.isAbsolute()) {
            return baseDirectory;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(baseDirectory);
    }

}