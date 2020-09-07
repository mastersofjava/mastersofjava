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
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.submit.service.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitService;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.util.Assert;

/**
 * During integration testing this class is executed twice, one for sequential and one for parallel.
 * This test validates the Assignment Submit.
 * - with a long timeout ==> user gets zero points (because the solution is invalidated by timeout constraints)
 * - without timeout on first submit ==> users gets a score (while competition running)
 * - user submits in last second and process takes more than second ==> user gets a score (while competition not running)
 */
@RunWith(Parameterized.class)
@SpringBootTest
public class AssignmentSubmitTest extends BaseRuntimeTest {
    // NB. these final parameters belong to the RunWith Parameterized configuration.
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitService submitService;

    @Autowired
    private MojServerProperties mojServerProperties;

    private String assignment;

    @Parameterized.Parameters(name = "{index} = {0}")
    public static String[] data() {
        return new String[]{
                "sequential"
                ,"parallel"
        };
    }

    public AssignmentSubmitTest(String assignment) {
        this.assignment = assignment; // sequential or parallel
    }

    private SourceMessage createSourceMessageWithLongTimeout(Duration timeout) {
        ActiveAssignment state = competitionRuntime.getActiveAssignment();

        Map<String, String> variables = new HashMap<>();
        if (timeout!=null) {
            variables.put("wait", Long.toString(timeout.toMillis()));
        }
        Map<String, String> files = getAssignmentFiles(state, variables);

        SourceMessage src = new SourceMessage();
        src.setSources(files);
        src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

        return src;
    }
    private void insertArrivalDetails(SourceMessage src, long timestamp) {
        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        src.setAssignmentName(state.getAssignment().getName());
        src.setUuid(state.getCompetitionSession().getUuid().toString());
        src.setTimeLeft("0");
        src.setArrivalTime(timestamp);
        Assert.isTrue(state.getCompetitionSession().getUuid().toString()!=null,"session is unknown");
    }
    private SourceMessage createSourceMessageWithNoTimeout() {
        return createSourceMessageWithLongTimeout(null);
    }
    private Duration createDurationThatIsLarge() {
        Duration timeout = competitionRuntime.getActiveAssignment().getAssignmentDescriptor().getTestTimeout();
        return timeout.plus(mojServerProperties.getLimits().getCompileTimeout());
    }
    private void startSelectedAssignmment() {
        OrderedAssignment oa = getAssignment(assignment);

        competitionRuntime.startAssignment(oa.getAssignment().getName());
    }
    private SubmitResult doValidate(SourceMessage src,Duration timeout) throws Exception {
        return submitService.test(SubmitRequest
                .builder()
                .user(getUser())
                .team(getTeam())
                .sourceMessage(src)
                .build())
                .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);
    }
    private SubmitResult doSubmit(SourceMessage src,Duration timeout) throws Exception {
        return submitService.submit(SubmitRequest
                .builder()
                .user(getUser())
                .team(getTeam())
                .sourceMessage(src)
                .build())
                .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);
    }
    private void stopSelectedAssignment() {
        competitionRuntime.stopCurrentSession();
    }
    private void assertValidSubmit( SubmitResult submitResult) {
        assertThat(submitResult.isSuccess()).isTrue();
        assertThat(submitResult.getScore()).isGreaterThan(0);
    }
    private void assertInvalidSubmit( SubmitResult submitResult) {
        assertThat(submitResult.isSuccess()).isFalse();
        assertThat(submitResult.getScore()).isEqualTo(0);
    }
    @Test
    public void shouldUseSpecifiedAssignmentTestTimeout() throws Exception {
        startSelectedAssignmment();
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithLongTimeout(timeout);
        SubmitResult submitResult = doValidate(src, timeout);
        assertThat(submitResult.getTestResults().getResults().get(0).isSuccess()).isFalse();
        assertThat(submitResult.getTestResults().getResults().get(0).isTimeout()).isTrue();
    }

    @Test
    public void shouldGetPointsForSuccessOnFirstAttempt() throws Exception {
        startSelectedAssignmment();
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithNoTimeout();

        SubmitResult submitResult = doSubmit(src, timeout);
        assertValidSubmit(submitResult);
    }

    @Test
    public void shouldGetPointsForSuccessOnVeryLateAttempt() throws Exception {
        startSelectedAssignmment();
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithNoTimeout();
        insertArrivalDetails(src, Instant.now().toEpochMilli());
        stopSelectedAssignment();
        SubmitResult submitResult = doSubmit(src, timeout);
        assertValidSubmit(submitResult);
    }
    @Test
    public void shouldGetZeroPointsForSuccessOnTooLateAttempt() throws Exception {
         startSelectedAssignmment();
         Duration timeout = createDurationThatIsLarge();
         SourceMessage src = createSourceMessageWithNoTimeout();
         insertArrivalDetails(src, Instant.now().plusSeconds(60*10).toEpochMilli());
         stopSelectedAssignment();
         SubmitResult submitResult = doSubmit(src, timeout);
         assertInvalidSubmit(submitResult);
    }
}
