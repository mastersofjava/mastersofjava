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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.OrderedAssignment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AssignmentDurationTest extends BaseRuntimeTest {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AssignmentRuntime assignmentRuntime;

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Test
    public void shouldRunForSpecifiedDuration() throws Exception {
        OrderedAssignment oa = getAssignment("parallel");

        Assertions.assertThat(oa).isNotNull();

        AssignmentDescriptor ad = assignmentService.getAssignmentDescriptor(oa.getAssignment());

        Future<?> mainHandle = assignmentRuntime.start(oa, competitionRuntime.getCompetitionSession());

        try {
            mainHandle.get(ad.getDuration().toSeconds() + 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            mainHandle.cancel(true);
            Assertions.fail("Caught unexpected exception.", e);
        }
    }
}
