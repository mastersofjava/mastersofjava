package nl.moj.server.runtime;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.OrderedAssignment;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AssignmentDurationTest extends BaseRuntimeTest {

	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private AssignmentRuntime assignmentRuntime;

	@Autowired
	private CompetitionRuntime competitionRuntime;

	@Test
	@Ignore
	public void shouldRunForSpecifiedDuration() {
		OrderedAssignment oa = getCompetition().getAssignmentsInOrder().get(0);
		AssignmentDescriptor ad = assignmentService.getAssignmentDescriptor(oa.getAssignment());

		Future<?> stopHandle = assignmentRuntime.start(oa, competitionRuntime.getCompetitionSession());

		try {
			stopHandle.get(ad.getDuration().toSeconds() + 1, TimeUnit.SECONDS);
		} catch (Exception e) {
			Assertions.fail("Caught unexpected exception.", e);
		}
	}
}
