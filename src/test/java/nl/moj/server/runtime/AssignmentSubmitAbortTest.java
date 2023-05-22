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

import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.SubmitFacade;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.test.model.TestAttempt;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@SpringBootTest
@ActiveProfiles("test-controller")
public class AssignmentSubmitAbortTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitFacade submitFacade;

    @Autowired
    AssignmentService assignmentService;

    private static Stream<String> assignments() {
        return Stream.of("sequential", "parallel");
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldAbortOnNoResponseCompile(String assignment) {
        startSelectedAssignment(assignment);
        SourceMessage src = createSourceMessage();
        CompileAttempt compileAttempt = doCompile(src);
        assertAbort(compileAttempt);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldAbortOnNoResponseTest(String assignment) {
        startSelectedAssignment(assignment);
        SourceMessage src = createSourceMessage();
        TestAttempt testAttempt = doTest(src);
        assertAbort(testAttempt);
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void shouldAbortOnNoResponseSubmit(String assignment) {
        startSelectedAssignment(assignment);
        SourceMessage src = createSourceMessage();
        SubmitAttempt submitAttempt = doSubmit(src);
        assertAbort(submitAttempt);
    }

    private SourceMessage createSourceMessage() {
        ActiveAssignment state = competitionRuntime.getActiveAssignment();

        Map<String, String> variables = new HashMap<>();
        variables.put("wait", "100");
        Map<String, String> files = getAssignmentFiles(state, variables);

        SourceMessage src = new SourceMessage();
        src.setSources(files);
        src.setUuid(getTeam().getUuid().toString());
        src.setAssignmentName(state.getAssignment().getName());
        src.setTimeLeft(Long.toString(state.getTimeRemaining().toSeconds()));
        src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

        return src;
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
        Duration timeout = assignmentService.resolveTestAbortTimout(competitionRuntime.getActiveAssignment().getAssignment(),src.getTests().size()).plusSeconds(5);
        TestAttempt ta = submitFacade.registerTestRequest(src, getPrincipal(getUser()));
        if( ta != null ) {
            awaitAttempt(ta.getUuid(), timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return ta;
    }

    private SubmitAttempt doSubmit(SourceMessage src) {
        Duration timeout = assignmentService.resolveSubmitAbortTimout(competitionRuntime.getActiveAssignment().getAssignment()).plusSeconds(5);
        SubmitAttempt sa = submitFacade.registerSubmitRequest(src, getPrincipal(getUser()));
        if( sa != null ) {
            awaitAttempt(sa.getUuid(), timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        return sa;
    }
}
