package nl.moj.server.runtime;

import nl.moj.server.DbUtil;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static nl.moj.server.TestUtil.classpathResourceToPath;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AssignmentRuntimeTest {

	@Autowired
	private AssignmentRuntime assignmentRuntime;

	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private CompetitionRepository competitionRepository;

	@Autowired
	private CompetitionRuntime competitionRuntime;

	@Autowired
	private DbUtil dbUtil;

	private Competition competition;

	@Before
	public void init() {
		dbUtil.cleanup();
	    competition = createCompetition();
	    competitionRuntime.startCompetition(competition);
	}

	@Test
	public void shouldRunForSpecifiedDuration() {
		OrderedAssignment oa = competition.getAssignmentsInOrder().get(0);
		AssignmentDescriptor ad = assignmentService.getAssignmentDescriptor(oa.getAssignment());

		Future<?> stopHandle = assignmentRuntime.start(oa, competitionRuntime.getCompetitionSession());

		try {
			stopHandle.get(ad.getDuration().toSeconds() + 1, TimeUnit.SECONDS);
		} catch( Exception e ) {
			Assertions.fail("Caught unexpected exception.",e);
		}
	}

	public Competition createCompetition() {
		List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/runtime/assignments"));
		AtomicInteger count = new AtomicInteger(0);
		final Competition c = new Competition();
		c.setUuid(UUID.randomUUID());
		c.setName("Test");
		c.setAssignments(assignments.stream()
		.map( a -> {
			OrderedAssignment oa = new OrderedAssignment();
			oa.setCompetition(c);
			oa.setAssignment(a);
			oa.setOrder(count.getAndIncrement());
			oa.setUuid(UUID.randomUUID());
			return oa;
		}).collect(Collectors.toList()));

		return competitionRepository.save(c);
	}
}
