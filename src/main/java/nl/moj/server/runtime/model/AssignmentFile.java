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
package nl.moj.server.runtime.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import org.apache.tika.mime.MediaType;

@Data
@Builder(toBuilder = true)
public class AssignmentFile {

    private final UUID uuid;

    private final String name;

    private final String shortName;

    private final byte[] content;

    private final AssignmentFileType fileType;

    private final String assignment;

    private final Path absoluteFile;

    private final Path file;

    private final Path base;

    private final MediaType mediaType;

    private final boolean readOnly;

    public String getContentAsString() {
        return new String(getContent(), StandardCharsets.UTF_8);
    }
}
