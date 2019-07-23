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
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ParallelAssignmentSubmitTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitService submitService;

    @Autowired
    private MojServerProperties mojServerProperties;

    @Test
    public void shouldUseSpecifiedAssignmentTestTimeout() {

        OrderedAssignment oa = getAssignment("parallel");

        competitionRuntime.startAssignment(oa.getAssignment().getName());

        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
        timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("wait", Long.toString(timeout.toMillis()));
            Map<String, String> files = getAssignmentFiles(state, variables);

            SourceMessage src = new SourceMessage();
            src.setSources(files);
            src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));


            SubmitResult submitResult = submitService.test(getTeam(), src)
                    .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

            Assertions.assertThat(submitResult.getTestResults().getResults().get(0).isSuccess()).isFalse();
            Assertions.assertThat(submitResult.getTestResults().getResults().get(0).isTimeout()).isTrue();

        } catch (Exception e) {
            Assertions.fail("Caught unexpected exception.", e);
        }
    }

    @Test
    public void shouldGetPointsForSuccessOnFirstAttempt() {
        OrderedAssignment oa = getAssignment("parallel");

        competitionRuntime.startAssignment(oa.getAssignment().getName());

        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
        timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

        try {
            Map<String, String> files = getAssignmentFiles(state, new HashMap<>());

            SourceMessage src = new SourceMessage();
            src.setSources(files);
            src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

            SubmitResult submitResult = submitService.submit(getTeam(), src)
                    .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

            Assertions.assertThat(submitResult.isSuccess()).isTrue();
            Assertions.assertThat(submitResult.getScore()).isGreaterThan(0);

        } catch (Exception e) {
            Assertions.fail("Caught unexpected exception.", e);
        }
    }
}
