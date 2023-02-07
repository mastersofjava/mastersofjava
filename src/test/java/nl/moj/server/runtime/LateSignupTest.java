package nl.moj.server.runtime;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class LateSignupTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Test
    public void handleLateSignup() throws Exception {
        CompetitionAssignment oa = getAssignment("parallel");
        competitionRuntime.startAssignment(competitionRuntime.getCompetitionSession().getUuid(), oa.getAssignment()
                .getUuid());
        TeamAssignmentStatus status = competitionRuntime.handleLateSignup(addTeam());
        assertThat(status).isNotNull();
    }
}
