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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitService;
import org.jboss.logging.Param;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@SpringBootTest
public class AssignmentSubmitTest extends BaseRuntimeTest {

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
        return new String[]{"sequential","parallel"};
    }

    public AssignmentSubmitTest(String assignment) {
        this.assignment = assignment;
    }

    @Test
    public void shouldUseSpecifiedAssignmentTestTimeout() throws Exception {

        OrderedAssignment oa = getAssignment(assignment);

        competitionRuntime.startAssignment(oa.getAssignment().getName());

        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
        timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

        Map<String, String> variables = new HashMap<>();
        variables.put("wait", Long.toString(timeout.toMillis()));
        Map<String, String> files = getAssignmentFiles(state, variables);

        SourceMessage src = new SourceMessage();
        src.setSources(files);
        src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));


        SubmitResult submitResult = submitService.test(getTeam(), src)
                .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

        assertThat(submitResult.getTestResults().getResults().get(0).isSuccess()).isFalse();
        assertThat(submitResult.getTestResults().getResults().get(0).isTimeout()).isTrue();

    }

    @Test
    public void shouldGetPointsForSuccessOnFirstAttempt() throws Exception {
        OrderedAssignment oa = getAssignment(assignment);

        competitionRuntime.startAssignment(oa.getAssignment().getName());

        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
        timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

        Map<String, String> files = getAssignmentFiles(state, new HashMap<>());

        SourceMessage src = new SourceMessage();
        src.setSources(files);
        src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

        SubmitResult submitResult = submitService.submit(getTeam(), src)
                .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

        assertThat(submitResult.isSuccess()).isTrue();
        assertThat(submitResult.getScore()).isGreaterThan(0);
    }
}
