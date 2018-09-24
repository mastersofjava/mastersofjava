package nl.moj.server.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.CompetitionState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionRuntime {


	private final AssignmentRuntime assignmentRuntime;

	private final AssignmentService assignmentService;

	private final TeamRepository teamRepository;

	private final CompetitionSessionRepository competitionSessionRepository;

	@Getter
	private Competition competition;

	@Getter
	private CompetitionSession competitionSession;

	private List<OrderedAssignment> completedAssignments;

	public void startCompetition(Competition competition) {
		log.info("Starting competition {}", competition.getName());
		this.competition = competition;
		this.competitionSession = competitionSessionRepository.save(createNewCompetitionSession(competition));
		this.completedAssignments = new ArrayList<>();
	}

	public OrderedAssignment getCurrentAssignment() {
		if (assignmentRuntime.isRunning()) {
			return assignmentRuntime.getOrderedAssignment();
		}
		return null;
	}

	public CompetitionState getCompetitionState() {
		if (competitionSession != null) {
			return CompetitionState.builder()
					.completedAssignments(completedAssignments)
					.build();
		}
		return CompetitionState.builder().build();
	}

	public AssignmentState getAssignmentState() {
		return assignmentRuntime.getState();
	}

	public void startAssignment(String name) {
		stopCurrentAssignment();
		competition.getAssignments().stream()
				.filter(a -> a.getAssignment().getName().equals(name))
				.forEach(a -> {
					assignmentRuntime.start(a, competitionSession);
					if (!completedAssignments.contains(a)) {
						completedAssignments.add(a);
					}
				});
	}

	public void stopCurrentAssignment() {
		if (assignmentRuntime.getOrderedAssignment() != null) {
			assignmentRuntime.stop();
		}
	}

	private CompetitionSession createNewCompetitionSession(Competition competition) {
		CompetitionSession competitionSession = new CompetitionSession();
		competitionSession.setUuid(UUID.randomUUID());
		competitionSession.setCompetition(competition);
		return competitionSession;
	}

	public List<ImmutablePair<String, Long>> getAssignmentInfo() {
		if (competition == null) {
			return Collections.emptyList();
		}

		return Optional.ofNullable(competition.getAssignments()).orElse(Collections.emptyList()).stream()
				.map(v -> {
					AssignmentDescriptor ad = assignmentService.getAssignmentDescriptor(v.getAssignment());
					return ImmutablePair.of(ad.getName(), ad.getDuration().toSeconds());
				}).sorted().collect(Collectors.toList());
	}

	public void registerAssignmentCompleted(Team team, Long timeScore, Long finalScore) {
		if (assignmentRuntime.getOrderedAssignment() != null) {
			assignmentRuntime.registerAssignmentCompleted(team, timeScore, finalScore);
		}
	}

	public void registerSubmit(Team team) {
		if (assignmentRuntime.getOrderedAssignment() != null) {
			assignmentRuntime.registerSubmitForTeam(team);
		}
	}

	public List<AssignmentFile> getTeamAssignmentFiles(Team team) {
		if (assignmentRuntime.getOrderedAssignment() != null) {
			return assignmentRuntime.getTeamAssignmentFiles(team);
		}
		return Collections.emptyList();
	}
}
