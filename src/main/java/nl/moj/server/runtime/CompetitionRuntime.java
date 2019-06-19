package nl.moj.server.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import nl.moj.server.runtime.model.AssignmentStatus;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.CompetitionState;
import nl.moj.server.teams.model.Team;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionRuntime {

	private final AssignmentRuntime assignmentRuntime;

	private final AssignmentService assignmentService;

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

	public ActiveAssignment getActiveAssignment() {
		return assignmentRuntime.getState();
	}

	public void startAssignment(String name) {
		log.debug("stopping current assignment to start assignment '{}'", name);
		stopCurrentAssignment();
		Optional<OrderedAssignment> assignment = competition.getAssignments().stream()
				.filter(a -> a.getAssignment().getName().equals(name))
				.findFirst();
		
		if (assignment.isPresent()) {
			assignmentRuntime.start(assignment.get(), competitionSession);
			if (!completedAssignments.contains(assignment.get())) {
				completedAssignments.add(assignment.get());
			}
		} else {
			log.error("Cannot start assignment '{}' since there is no such assignment with that name", name);
		}
	}

	public void stopCurrentAssignment() {
		if (assignmentRuntime.getOrderedAssignment() != null) {
			assignmentRuntime.stop();
		}
	}

	private CompetitionSession createNewCompetitionSession(Competition competition) {
		var newCompetitionSession = new CompetitionSession();
		newCompetitionSession.setUuid(UUID.randomUUID());
		newCompetitionSession.setCompetition(competition);
		return newCompetitionSession;
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

	public List<AssignmentFile> getTeamAssignmentFiles(Team team) {
		if (assignmentRuntime.getOrderedAssignment() != null) {
			return assignmentRuntime.getTeamAssignmentFiles(team);
		}
		return Collections.emptyList();
	}
	
	public AssignmentStatus handleLateSignup(Team team) {
		return assignmentRuntime.initAssignmentForLateTeam(team);
	}	
}
