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

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.message.service.MessageService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NonExistingJDKTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @MockBean
    private MessageService messageService;

    @Test
    public void assignmentShouldNotStartWhenJDKVersionUnavailable() {
        OrderedAssignment oa = getAssignment("non-existing-jdk");
        competitionRuntime.startAssignment(oa.getAssignment().getName());
        Mockito.verify(messageService).sendStartFail(eq("non-existing-jdk"), any());
    }
}
