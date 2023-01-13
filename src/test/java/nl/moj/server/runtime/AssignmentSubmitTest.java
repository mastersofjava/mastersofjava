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
import java.util.stream.Stream;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.service.CompileRequest;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.SubmitController;
import nl.moj.server.submit.SubmitFacade;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.service.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.test.model.TestAttempt;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

/**
 * During integration testing this class is executed twice, one for sequential and one for parallel.
 * This test validates the Assignment Submit.
 * - with a long timeout ==> user gets zero points (because the solution is invalidated by timeout constraints)
 * - without timeout on first submit ==> users gets a score (while competition running)
 * - user submits in last second and process takes more than second ==> user gets a score (while competition not running)
 */
@SpringBootTest
public class AssignmentSubmitTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitFacade submitFacade;

    @Autowired
    private MojServerProperties mojServerProperties;

    private static Stream<String> assignments() {
        return Stream.of("sequential","parallel");
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
    private void startSelectedAssignment(String assignment) {
        OrderedAssignment oa = getAssignment(assignment);
        competitionRuntime.startAssignment(oa.getAssignment().getName());
    }

    private CompileAttempt doCompile(SourceMessage src, Duration timeout) throws Exception {
        CompileAttempt ca = submitFacade.registerCompileRequest(src,getPrincipal(getUser()));
        awaitAttempt(ca.getUuid(),timeout.toMillis(), TimeUnit.MILLISECONDS);
        return refresh(ca);
    }

    private TestAttempt doTest(SourceMessage src, Duration timeout) throws Exception {
        TestAttempt ta = submitFacade.registerTestRequest(src,getPrincipal(getUser()));
        awaitAttempt(ta.getUuid(),timeout.toMillis(), TimeUnit.MILLISECONDS);
        return refresh(ta);
    }

    private SubmitAttempt doSubmit(SourceMessage src,Duration timeout) throws Exception {
        SubmitAttempt sa = submitFacade.registerSubmitRequest(src,getPrincipal(getUser()));
        awaitAttempt(sa.getUuid(),timeout.toMillis(), TimeUnit.MILLISECONDS);
        return refresh(sa);
    }

    private void stopSelectedAssignment() {
        competitionRuntime.stopCurrentSession();
    }

    private void assertValidSubmit( SubmitAttempt submitResult) {
        assertThat(submitResult.isSuccess()).isTrue();
        //assertThat(submitResult.getScore()).isGreaterThan(0);
    }
    private void assertInvalidSubmit( SubmitAttempt submitResult) {
        assertThat(submitResult.isSuccess()).isFalse();
        //assertThat(submitResult.getScore()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldCompile(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithNoTimeout();
        CompileAttempt compileAttempt = doCompile(src, timeout);

        //assertValidSubmit(submitResult);
        Assertions.assertThat(compileAttempt).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldTest(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithNoTimeout();
        TestAttempt testAttempt = doTest(src, timeout);

        //assertValidSubmit(submitResult);
        Assertions.assertThat(testAttempt).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("assignments")
    @Disabled
    public void shouldUseSpecifiedAssignmentTestTimeout(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithLongTimeout(timeout);
        TestAttempt testAttempt = doTest(src, timeout);
//        assertThat(testAttempt.getTestResults().getResults().get(0).isSuccess()).isFalse();
//        assertThat(testAttempt.getTestResults().getResults().get(0).isTimeout()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("assignments")
    @Disabled
    public void shouldGetPointsForSuccessOnFirstAttempt(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithNoTimeout();
        SubmitAttempt submitResult = doSubmit(src, timeout);
        assertValidSubmit(submitResult);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    @Disabled
    public void shouldGetPointsForSuccessOnVeryLateAttempt(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        Duration timeout = createDurationThatIsLarge();
        SourceMessage src = createSourceMessageWithNoTimeout();
        insertArrivalDetails(src, Instant.now().toEpochMilli());
        stopSelectedAssignment();
        SubmitAttempt submitResult = doSubmit(src, timeout);
        assertValidSubmit(submitResult);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    @Disabled
    public void shouldGetZeroPointsForSuccessOnTooLateAttempt(String assignment) throws Exception {
         startSelectedAssignment(assignment);
         Duration timeout = createDurationThatIsLarge();
         SourceMessage src = createSourceMessageWithNoTimeout();
         insertArrivalDetails(src, Instant.now().plusSeconds(60*10).toEpochMilli());
         stopSelectedAssignment();
         SubmitAttempt submitResult = doSubmit(src, timeout);
         assertInvalidSubmit(submitResult);
    }
}
