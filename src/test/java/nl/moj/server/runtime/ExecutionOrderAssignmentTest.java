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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.SubmitFacade;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.user.model.User;
import nl.moj.server.util.CompletableFutures;

@SpringBootTest
public class ExecutionOrderAssignmentTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitFacade submitFacade;

    @Autowired
    private MojServerProperties mojServerProperties;

    private SourceMessage createWithDelay(String wait, ActiveAssignment state) {
        Map<String, String> variables = new HashMap<>();
        variables.put("wait", wait);
        Map<String, String> files = getAssignmentFiles(state, variables);

        SourceMessage src1 = new SourceMessage();
        src1.setSources(files);
        src1.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));
        return src1;
    }

    private TestAttempt findSubmitResultByTeam(List<TestAttempt> results, Team team1) {
        return results.stream()
                .filter(r -> r.getAssignmentStatus().getTeam().getUuid().equals(team1.getUuid()))
                .findFirst()
                .orElse(null);
    }

    @Test
    public void sequentialExecutionShouldHaveNoOverlappingExecutionWindows() {

        Team team1 = getTeam();
        User user1 = getUser();
        Team team2 = addTeam();
        User user2 = addUser(team2);

        try {
            CompetitionAssignment oa = getAssignment("sequential");
            competitionRuntime.startAssignment(competitionRuntime.getSessionId(), oa.getAssignment()
                    .getUuid());
            ActiveAssignment state = competitionRuntime.getActiveAssignment(null);
            Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
            timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

            // make sure team 1 runs for 0.1s and one for 0.5

            SourceMessage src1 = createWithDelay("100", state);
            SourceMessage src2 = createWithDelay("50", state);

            CompletableFuture<List<TestAttempt>> prepareForSubmit = CompletableFutures.allOf(
                    doTest(src1, user1, Duration.ofSeconds(5)),
                    doTest(src2, user2, Duration.ofSeconds(5)));
            // run them all at once and wait for the results.
            List<TestAttempt> results = prepareForSubmit.get(timeout.plusSeconds(20).toSeconds(), TimeUnit.SECONDS);

            // sequential execution means just a single completable future can run at any given time
            // this means there is no guarantee in the order, but none will run in the same time.
            TestAttempt t1result = findSubmitResultByTeam(results, team1);
            TestAttempt t2result = findSubmitResultByTeam(results, team2);

            assertThat(t1result).isNotNull();
            assertThat(t2result).isNotNull();

            // test that there is no overlap in execution windows.
            assertNoOverlappingExecutionWindows(t1result, t2result);

        } catch (Exception e) {
            Assertions.fail("Caught unexpected exception.", e);
        }
    }

    // no clue why this isn't working
    @Test
    @Disabled
    public void parallelExecutionShouldHaveOverlappingExecutionWindows() {

        Team team1 = getTeam();
        User user1 = getUser();
        Team team2 = addTeam();
        User user2 = addUser(team2);

        try {
            CompetitionAssignment oa = getAssignment("parallel");
            competitionRuntime.startAssignment(competitionRuntime.getSessionId(), oa.getAssignment()
                    .getUuid());

            ActiveAssignment state = competitionRuntime.getActiveAssignment(null);
            Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
            timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

            // make sure team 1 runs for 1s and one for 0.5

            SourceMessage src1 = createWithDelay("1000", state);
            SourceMessage src2 = createWithDelay("500", state);

            CompletableFuture<List<TestAttempt>> prepareForSubmit = CompletableFutures.allOf(
                    doTest(src1, user1, Duration.ofSeconds(5)),
                    doTest(src2, user2, Duration.ofSeconds(5)));
            // run them all at once and wait for the results.
            List<TestAttempt> results = prepareForSubmit.get(timeout.plusSeconds(20).toSeconds(), TimeUnit.SECONDS);

            // sequential execution means just a single completable future can run at any given time
            // this means there is no guarantee in the order, but none will run in the same time.
            TestAttempt t1result = findSubmitResultByTeam(results, team1);
            TestAttempt t2result = findSubmitResultByTeam(results, team2);

            assertThat(t1result).isNotNull();
            assertThat(t2result).isNotNull();

            // test that there is no overlap in execution windows.
            assertOverlappingExecutionWindows(t1result, t2result);

        } catch (Exception e) {
            Assertions.fail("Caught unexpected exception.", e);
        }
    }

    private CompletableFuture<TestAttempt> doTest(SourceMessage src, User user, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            TestAttempt ta = submitFacade.registerTestRequest(src, getPrincipal(user));
            awaitAttempt(ta.getUuid(), timeout.toMillis(), TimeUnit.MILLISECONDS);
            return refresh(ta);
        });
    }
}
