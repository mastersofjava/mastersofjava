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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.teams.model.Team;
import nl.moj.server.util.CompletableFutures;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SequentialAssignmentSubmitTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitService submitService;

    @Autowired
    private MojServerProperties mojServerProperties;

    @Test
    public void sequentialExecutionShouldHaveNoOverlappingExecutionWindows() {

        Team team1 = getTeam();
        Team team2 = addTeam();

        OrderedAssignment oa = getAssignment("sequential");

        competitionRuntime.startAssignment(oa.getAssignment().getName());

        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
        timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

        try {
            // make sure team 1 runs for 0.1s
            Map<String, String> variables = new HashMap<>();
            variables.put("wait", "100");
            Map<String, String> files = getAssignmentFiles(state, variables);

            SourceMessage src1 = new SourceMessage();
            src1.setSources(files);
            src1.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

            // make sure team 2 runs for 0.05s
            variables = new HashMap<>();
            variables.put("wait", "50");
            files = getAssignmentFiles(state, variables);

            SourceMessage src2 = new SourceMessage();
            src2.setSources(files);
            src2.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

            // run them all at once and wait for the results.
            List<SubmitResult> results = CompletableFutures.allOf(submitService.test(team1, src1), submitService.test(team2, src2))
                    .get(timeout.plusSeconds(20).toSeconds(), TimeUnit.SECONDS);

            // sequential execution means just a single completable future can run at any given time
            // this means there is no guarantee in the order, but none will run in the same time.
            SubmitResult t1result = results.stream()
                    .filter(r -> r.getTeam().equals(team1.getUuid()))
                    .findFirst()
                    .orElse(null);
            SubmitResult t2result = results.stream()
                    .filter(r -> r.getTeam().equals(team2.getUuid()))
                    .findFirst()
                    .orElse(null);

            assertThat(t1result).isNotNull();
            assertThat(t2result).isNotNull();

            // test that there is no overlap in execution windows.
            assertNoOverlappingExecutionWindows(t1result, t2result);


        } catch (Exception e) {
            Assertions.fail("Caught unexpected exception.", e);
        }
    }
}
