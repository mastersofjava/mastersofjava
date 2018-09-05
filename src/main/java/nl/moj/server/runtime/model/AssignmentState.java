package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;

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

	public List<AssignmentFile> getSubmitFiles() {
		return assignmentFiles
				.stream().filter(f -> f.getFileType() == AssignmentFileType.SUBMIT)
				.collect(Collectors.toList());
	}

	public boolean isTeamFinished(String team) {
		return finishedTeams.stream().anyMatch( t -> t.getTeam().getName().equals(team));
	}
}
