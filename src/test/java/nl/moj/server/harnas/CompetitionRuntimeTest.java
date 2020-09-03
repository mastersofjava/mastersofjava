package nl.moj.server.harnas;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.runtime.AssignmentRuntime;
import nl.moj.server.runtime.BaseRuntimeTest;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class CompetitionRuntimeTest extends BaseRuntimeTest {
    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Test
    public void useCompetitionRuntimeDuringSampleStartup() throws Exception {
        assertThat(competitionRuntime.getActiveCompetitionsMap().size()).isGreaterThan(1);/// flacky value

        assertThat(competitionRuntime.getCompetition().getShortName()).isSameAs("TestCase");
        assertThat(competitionRuntime.getSessions().size()).isEqualTo(1);
        assertThat(competitionRuntime.getAssignmentInfo().size()).isEqualTo(4);
        assertThat(competitionRuntime.getAssignmentInfoOrderedForCompetition().size()).isEqualTo(4);
        UUID assignmentUuid = competitionRuntime.getCompetition().getAssignments().get(0).getAssignment().getUuid();
        assertThat(competitionRuntime.getSolutionFiles(assignmentUuid).size()).isEqualTo(0);
        assertThat(competitionRuntime.getTeamSolutionFiles(assignmentUuid,this.getTeam()).size()).isEqualTo(0);
        assertThat(competitionRuntime.getCompetitionState().getCompletedAssignments().size()).isEqualTo(0);
    }

    @Ignore
    @Test
    public void handleLateSignup() throws Exception {
        String name = competitionRuntime.getCompetition().getAssignments().get(0).getAssignment().getName();
        competitionRuntime.startAssignment(name);
        UUID uuid = competitionRuntime.getCompetitionSession().getUuid();
        AssignmentStatus status = competitionRuntime.handleLateSignup(addTeam(), uuid, name);
        assertThat(status).isNotNull();
        OrderedAssignment assignment = competitionRuntime.determineNextAssignmentIfAny();
        assertThat(assignment.getAssignment()).isNotEqualTo(competitionRuntime.getCompetition().getAssignments().get(0).getAssignment());
    }
}
