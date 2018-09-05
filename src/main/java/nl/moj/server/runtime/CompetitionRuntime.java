package nl.moj.server.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.TeamStatus;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionRuntime {


	private final AssignmentRuntime assignmentRuntime;

	private final AssignmentService assignmentService;

	private final TeamRepository teamRepository;

	@Getter
	private Competition competition;

	public void initializeCompetition(Competition competition) {
		log.info("Initializing competition {}", competition.getName());
		this.competition = competition;
	}

	public OrderedAssignment getCurrentAssignment() {
		if (assignmentRuntime.isRunning()) {
			return assignmentRuntime.getOrderedAssignment();
		}
		return null;
	}

	public AssignmentState getAssignmentState() {
		return assignmentRuntime.getState();
	}

	public void startAssignment(String name) {
		competition.getAssignments().stream()
				.filter(a -> a.getAssignment().getName().equals(name))
				.forEach(assignmentRuntime::start);
	}

	public void stopCurrentAssignment() {
		assignmentRuntime.stop();
	}

	public List<String> getAssignmentNames() {
		return competition.getAssignments().stream()
				.map(a -> a.getAssignment().getName()).collect(Collectors.toList());
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

	public void registerFinishedTeam(String user, Long submissionTime, Long finalScore) {
		if( assignmentRuntime.getOrderedAssignment() != null ) {
			Team team = teamRepository.findByName(user);
			assignmentRuntime.addFinishedTeam(TeamStatus.builder()
					.team(team)
					.score(finalScore)
					.submitTime(submissionTime)
					.build());
		}
	}

	public List<AssignmentFile> getTeamAssignmentFiles(Team team) {
		if( assignmentRuntime.getOrderedAssignment() != null ) {
			return assignmentRuntime.getTeamAssignmentFiles(team);
		}
		return Collections.emptyList();
	}
}
