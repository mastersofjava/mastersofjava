package nl.moj.server.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
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
public class CustomSecurityPolicyTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitService submitService;

    @Autowired
    private MojServerProperties mojServerProperties;

    @Test
    public void shouldUseAssignmentSecurityPolicy() throws Exception {

        OrderedAssignment oa = getCompetition().getAssignments()
                .stream()
                .filter(a -> a.getAssignment().getName().equals("custom-security-policy"))
                .findFirst()
                .orElseThrow();

        competitionRuntime.startAssignment(oa.getAssignment().getName());

        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
        timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

        Map<String, String> files = state.getAssignmentFiles().stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT)
                .collect(Collectors.toMap(f -> f.getUuid().toString(), AssignmentFile::getContentAsString));

        SourceMessage src = new SourceMessage();
        src.setSources(files);
        src.setTests(List.of(state.getTestFiles().get(0).getUuid().toString()));


        SubmitResult submitResult = submitService.test(getTeam(), src)
                .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

        Assertions.assertThat(submitResult.isSuccess()).isTrue();
        Assertions.assertThat(submitResult.getTestResults().getResults().get(0).isSuccess()).isTrue();
        Assertions.assertThat(submitResult.getTestResults().getResults().get(0).isTimeout()).isFalse();
    }
}
