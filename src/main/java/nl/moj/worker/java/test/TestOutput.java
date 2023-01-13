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
package nl.moj.worker.java.test;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TestOutput {

    private UUID testCase;

    private String output;
    private String errorOutput;
    @Builder.Default
    private boolean success = false;
    @Builder.Default
    private boolean timedOut = false;
    @Builder.Default
    private boolean aborted = false;
    private String reason;

    private Instant dateTimeStart;
    private Instant dateTimeEnd;
}
