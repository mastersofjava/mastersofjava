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
package nl.moj.server.runtime;

import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.server.message.service.MessageService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class NonExistingJDKTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @MockBean
    private MessageService messageService;

    // TODO should be fixed once workers report capabilities.
    // For now worker nodes just consume messages and we hope for the best.
    @Test
    @Disabled
    public void assignmentShouldNotStartWhenJDKVersionUnavailable() {
        Assertions.assertThatExceptionOfType(CompetitionServiceException.class).isThrownBy(() -> {
            CompetitionAssignment oa = getAssignment("non-existing-jdk");
            competitionRuntime.startAssignment(competitionRuntime.getSessionId(), oa.getAssignment()
                    .getUuid());
        });
    }
}
