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
package nl.moj.common.config;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lombok.Data;

public class Directories {
    public static final String SESSIONS = "sessions";
    public static final String TEAMS = "teams";
    public static final String LIBS = "lib";
    public static final String SOUNDS = "sounds";
    public static final String JAVADOC = "javadoc";
    public static final String ASSIGNMENTS = "assignments";

    public static Path getSessions(Path base) {
        return base.resolve(SESSIONS);
    }
}