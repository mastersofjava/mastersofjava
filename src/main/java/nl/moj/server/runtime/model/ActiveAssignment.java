package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.teams.model.Team;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@Slf4j
public class ActiveAssignment {

	private CompetitionSession competitionSession;
	private Assignment assignment;
	private AssignmentDescriptor assignmentDescriptor;

	private Duration timeElapsed;
	private Long timeRemaining;
	private List<AssignmentFile> assignmentFiles;
	private boolean running;

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
				.stream().filter(f -> f.getFileType() == AssignmentFileType.HIDDEN_TEST ||
						f.getFileType() == AssignmentFileType.TEST)
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getVisibleFiles() {
		return assignmentFiles
				.stream().filter( f -> f.getFileType().isVisible())
				.collect(Collectors.toList());
	}
}
