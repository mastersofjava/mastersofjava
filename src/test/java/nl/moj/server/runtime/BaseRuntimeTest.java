package nl.moj.server.runtime;

import lombok.Getter;
import nl.moj.server.BootstrapService;
import nl.moj.server.DbUtil;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.PathUtil;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static nl.moj.server.TestUtil.classpathResourceToPath;

public abstract class BaseRuntimeTest {

	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private CompetitionRepository competitionRepository;

	@Autowired
	private CompetitionRuntime competitionRuntime;

	@Autowired
	private DbUtil dbUtil;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private MojServerProperties mojServerProperties;

	@Autowired
	private BootstrapService bootstrapService;

	@Getter
	private Competition competition;

	@Getter
	private Team team;

	@Before
	public void init() throws IOException {
		bootstrapService.bootstrap();
		dbUtil.cleanup();
		competition = createCompetition();
		competitionRuntime.startCompetition(competition);
	}

	@After
	public void cleanup() throws IOException {
		PathUtil.delete( mojServerProperties.getDirectories().getBaseDirectory(), true);
		dbUtil.cleanup();
	}

	protected Team addTeam() {
		Team team = new Team();
		team.setUuid(UUID.randomUUID());
		team.setName(team.getUuid().toString());
		team.setRole(Role.ROLE_USER);
		return teamRepository.save(team);
	}

	private Competition createCompetition() {
		team = addTeam();

		List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/runtime/assignments"));
		AtomicInteger count = new AtomicInteger(0);
		final Competition c = new Competition();
		c.setUuid(UUID.randomUUID());
		c.setName("Test");
		c.setAssignments(assignments.stream()
				.map(a -> {
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
