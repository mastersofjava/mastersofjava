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
package nl.moj.server.submit.service;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.test.service.TestsOutput;

@Builder(toBuilder = true)
@Getter
public class SubmitResult {

    private final UUID team;

    private final Instant dateTimeStart;

    private final Instant dateTimeEnd;

    @Builder.Default
    private final int remainingSubmits = 0;

    @Builder.Default
    private final long score = 0L;

    private CompileResult compileResult;

    private TestsOutput testResults;

    @Builder.Default
    private boolean success = false;
}
