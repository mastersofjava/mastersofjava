package nl.moj.server.runtime;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import org.junit.jupiter.api.Disabled;
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
        String name = "parallel";
        competitionRuntime.startAssignment(name);
        TeamAssignmentStatus status = competitionRuntime.handleLateSignup(addTeam());
        assertThat(status).isNotNull();
    }
}
