package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.teams.model.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@Slf4j
public class AssignmentState {

	private AssignmentDescriptor assignmentDescriptor;
	private Long timeRemaining;
	private List<AssignmentFile> assignmentFiles;
	private boolean running;

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

}
