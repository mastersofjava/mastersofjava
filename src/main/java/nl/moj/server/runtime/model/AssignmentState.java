package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.teams.model.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@Slf4j
public class AssignmentState {

	private Assignment assignment;
	private AssignmentDescriptor assignmentDescriptor;
	private Long timeRemaining;
	private List<AssignmentFile> assignmentFiles;
	private boolean running;

	@Builder.Default
	private Map<Team, TeamStatus> teamStatuses = new HashMap<>();

	public List<String> getTestNames() {
		return assignmentFiles
				.stream().filter(f -> f.getFileType() == AssignmentFileType.TEST)
				.map(AssignmentFile::getName).collect(Collectors.toList());
	}

	public List<AssignmentFile> getTestFiles() {
		return assignmentFiles
				.stream().filter(f -> f.getFileType() == AssignmentFileType.TEST)
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getSubmitTestFiles() {
		return assignmentFiles
				.stream().filter(f -> f.getFileType() == AssignmentFileType.SUBMIT ||
						f.getFileType() == AssignmentFileType.TEST)
				.collect(Collectors.toList());
	}


	public TeamStatus getTeamStatus(Team team) {
		return teamStatuses.get(team);
	}

	public boolean isSubmitAllowedForTeam(Team team) {
		TeamStatus s = getTeamStatus(team);
		return !s.isCompleted() && s.getSubmits() < assignmentDescriptor.getScoringRules().getMaximumResubmits() + 1;
	}

	public int getRemainingSubmits(Team team) {
		TeamStatus s = getTeamStatus(team);
		return assignmentDescriptor.getScoringRules().getMaximumResubmits() + 1 - s.getSubmits();
	}
}
