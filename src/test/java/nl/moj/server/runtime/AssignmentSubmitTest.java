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

import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.SubmitFacade;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.test.model.TestAttempt;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * During integration testing this class is executed twice, one for sequential and one for parallel.
 * This test validates the Assignment Submit.
 * - with a long timeout ==> user gets zero points (because the solution is invalidated by timeout constraints)
 * - without timeout on first submit ==> users gets a score (while session running)
 * - user submits in last second and process takes more than second ==> user gets a score (while session not running)
 */
@SpringBootTest
public class AssignmentSubmitTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitFacade submitFacade;

    @Autowired
    private AssignmentService assignmentService;

    private static Stream<String> assignments() {
        return Stream.of("sequential", "parallel");
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldCompile(String assignment) {
        startSelectedAssignment(assignment);
        SourceMessage src = createSourceMessageWithNoTimeout();
        CompileAttempt compileAttempt = doCompile(src);

        assertSuccess(compileAttempt);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldTest(String assignment) {
        startSelectedAssignment(assignment);
        SourceMessage src = createSourceMessageWithNoTimeout();
        TestAttempt testAttempt = doTest(src);
        assertSuccess(testAttempt);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldUseSpecifiedAssignmentTestTimeout(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        Duration timeout = assignmentService.resolveTestAbortTimout(competitionRuntime.getActiveAssignment().getAssignment(),1);
        SourceMessage src = createSourceMessageWithLongTimeout(timeout);
        TestAttempt testAttempt = doTest(src);
        assertTimeout(testAttempt);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldGetPointsForSuccessOnFirstAttempt(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        SourceMessage src = createSourceMessageWithNoTimeout();
        SubmitAttempt submitAttempt = doSubmit(src);

        assertSuccess(submitAttempt);
        assertFinalScore(submitAttempt).isGreaterThan(0);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldGetZeroPointsForSuccessOnTooLateAttempt(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        SourceMessage src = createSourceMessageWithNoTimeout();

        Duration timeout = assignmentService.resolveSubmitAbortTimout(competitionRuntime.getActiveAssignment().getAssignment()).plusSeconds(5);
        stopSelectedAssignment();

        SubmitAttempt submitAttempt = doSubmit(src,timeout);

        Assertions.assertThat(submitAttempt).isNull();
        assertFinalScore(src).isEqualTo(0);
    }

    private SourceMessage createSourceMessageWithLongTimeout(Duration timeout) {
        ActiveAssignment state = competitionRuntime.getActiveAssignment();

        Map<String, String> variables = new HashMap<>();
        if (timeout != null) {
            variables.put("wait", Long.toString(timeout.toMillis()));
        }
        Map<String, String> files = getAssignmentFiles(state, variables);

        SourceMessage src = new SourceMessage();
        src.setSources(files);
        src.setUuid(getTeam().getUuid().toString());
        src.setAssignmentName(state.getAssignment().getName());
        src.setTimeLeft(""+state.getTimeRemaining().toSeconds());
        src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

        return src;
    }

    private SourceMessage createSourceMessageWithNoTimeout() {
        return createSourceMessageWithLongTimeout(null);
    }

    private void startSelectedAssignment(String assignment) {
        try {
            CompetitionAssignment oa = getAssignment(assignment);
            competitionRuntime.startAssignment(competitionRuntime.getSessionId(), oa.getAssignment()
                    .getUuid());
        } catch( CompetitionServiceException cse ) {
            throw new RuntimeException(cse);
        }
    }

    private CompileAttempt doCompile(SourceMessage src) {
        Duration timeout = assignmentService.resolveCompileAbortTimout(competitionRuntime.getActiveAssignment().getAssignment()).plusSeconds(5);
        CompileAttempt ca = submitFacade.registerCompileRequest(src, getPrincipal(getUser()));
        if( ca != null ) {
            awaitAttempt(ca.getUuid(), timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return ca;
    }

    private TestAttempt doTest(SourceMessage src) {
        return doTest(src,null);
    }

    private TestAttempt doTest(SourceMessage src, Duration d) {
        Duration timeout = d != null ? d : assignmentService.resolveTestAbortTimout(competitionRuntime.getActiveAssignment().getAssignment(), src.getTests().size())
                .plusSeconds(5);
        TestAttempt ta = submitFacade.registerTestRequest(src, getPrincipal(getUser()));
        if( ta != null ) {
            awaitAttempt(ta.getUuid(), timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return ta;
    }

    private SubmitAttempt doSubmit(SourceMessage src) {
        return doSubmit(src,null);
    }
    private SubmitAttempt doSubmit(SourceMessage src, Duration d) {
        Duration timeout = d != null ? d : assignmentService.resolveSubmitAbortTimout(competitionRuntime.getActiveAssignment().getAssignment()).plusSeconds(5);
        SubmitAttempt sa = submitFacade.registerSubmitRequest(src, getPrincipal(getUser()));
        if( sa != null ) {
            awaitAttempt(sa.getUuid(), timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return sa;
    }

    private void stopSelectedAssignment() throws Exception {
        if( competitionRuntime.getCurrentAssignment() != null ) {
            competitionRuntime.stopAssignment(getSessionId(), competitionRuntime.getCurrentAssignment());
        }
    }



}
