package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.teams.model.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class AssignmentState {

	private AssignmentDescriptor assignmentDescriptor;
	private Long timeRemaining;
	private List<AssignmentFile> assignmentFiles;
	private boolean running;
	
	@Builder.Default
	private List<TeamStatus> finishedTeams = new ArrayList<>();

	public List<String> getTestNames() {
		return assignmentFiles
				.stream().filter( f -> f.getFileType() == AssignmentFileType.TEST)
				.map( f -> f.getName() ).collect(Collectors.toList());
	}

	public List<AssignmentFile> getTestFiles() {
		return assignmentFiles
				.stream().filter(f -> f.getFileType() == AssignmentFileType.TEST)
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getHiddenTestFiles() {
		return assignmentFiles
				.stream().filter(f -> f.getFileType() == AssignmentFileType.HIDDEN_TEST)
				.collect(Collectors.toList());
	}

	public boolean isTeamFinished(Team team) {
		return finishedTeams.stream().anyMatch( t -> t.getTeam().equals(team));
	}

	public TeamStatus getTeamStatus(Team team) {
		return finishedTeams.stream().filter( t -> t.getTeam().equals(team)).findFirst().orElse(TeamStatus.builder().team(team).build());
	}
}
