package nl.moj.server.runtime;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.message.service.MessageService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NonExistingJDKTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @MockBean
    private MessageService messageService;

    @Test
    public void assignmentShouldNotStartWhenJDKVersionUnavailable() {

        OrderedAssignment oa = getCompetition().getAssignments()
                .stream()
                .filter(a -> a.getAssignment().getName().equals("non-existing-jdk"))
                .findFirst()
                .orElseThrow();

        competitionRuntime.startAssignment(oa.getAssignment().getName());
        Mockito.verify(messageService).sendStartFail(eq("non-existing-jdk"), any());
    }
}
