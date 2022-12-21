package nl.moj.server.compiler.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.submit.model.SourceMessage;

/**
 * Contains all data (references) required to be able to compile a submission.
 */
public class CompileInputWrapper {

	private final UUID compileAttemptUuid;
	private final UUID teamUuid;
	private final String teamName;
	private final Long teamId;
	private final Instant dateTimeSubmitted; 
	private final ActiveAssignment activeAssignment;
	private final SourceMessage sourceMessage;

	CompileInputWrapper(CompileRequest compileRequest, ActiveAssignment activeAssignment) {
		this.teamUuid = compileRequest.getTeam().getUuid();
		this.teamName = compileRequest.getTeam().getName();
		this.teamId = compileRequest.getTeam().getId();
		this.sourceMessage = compileRequest.getSourceMessage();
		this.compileAttemptUuid = UUID.randomUUID();
		this.dateTimeSubmitted = compileRequest.getDateTimeSubmitted();
		this.activeAssignment = activeAssignment;
    }

	public List<AssignmentFile> getReadonlyAssignmentFiles() {
		return getReadonlyAssignmentFilesToCompile(activeAssignment.getAssignmentFiles());
	}

	/**
	 * AssignmentFileType: READONLY, TEST, HIDDEN_TEST, HIDDEN
	 */
	private List<AssignmentFile> getReadonlyAssignmentFilesToCompile(List<AssignmentFile> fileList) {
		return fileList.stream().filter(f -> f.getFileType() == AssignmentFileType.READONLY
				|| f.getFileType() == AssignmentFileType.TEST || f.getFileType() == AssignmentFileType.HIDDEN_TEST
				|| f.getFileType() == AssignmentFileType.HIDDEN || f.getFileType() == AssignmentFileType.INVISIBLE_TEST)
				.collect(Collectors.toList());
	}

	private List<AssignmentFile> getResourcesToCopy(ActiveAssignment state) {
		return getResourcesToCopy(state.getAssignmentFiles());
	}

	public List<AssignmentFile> getResources() {
		return getResourcesToCopy(activeAssignment);
	}

	/**
	 * AssignmentFileType: RESOURCE, TEST_RESOURCE, HIDDEN_TEST_RESOURCE
	 */
	private List<AssignmentFile> getResourcesToCopy(List<AssignmentFile> fileList) {
		return fileList.stream()
				.filter(f -> f.getFileType() == AssignmentFileType.RESOURCE
						|| f.getFileType() == AssignmentFileType.TEST_RESOURCE
						|| f.getFileType() == AssignmentFileType.HIDDEN_TEST_RESOURCE
						|| f.getFileType() == AssignmentFileType.INVISIBLE_TEST_RESOURCE)
				.collect(Collectors.toList());
	}

	AssignmentFile getOriginalAssignmentFile(String uuid) {
		return activeAssignment.getAssignmentFiles().stream().filter(f -> f.getUuid().toString().equals(uuid)).findFirst()
				.orElseThrow(() -> new RuntimeException("Could not find original assignment file for UUID " + uuid));
	}

	public Assignment getAssignment() {
		return activeAssignment.getAssignment();
	}

	public UUID getCompileAttemptUuid() {
		return compileAttemptUuid;
	}

	public ActiveAssignment getActiveAssignment() {
		return activeAssignment;
	}

	public AssignmentDescriptor getAssignmentDescriptor() {
		return activeAssignment.getAssignmentDescriptor();
	}

	
	public Long getCompetitionSessionId() {
		return activeAssignment.getCompetitionSession().getId();
	}
	public SourceMessage getSourceMessage() {
		return sourceMessage;
	}

	public UUID getTeamUuid() {
		return teamUuid;
	}

	public Long getTeamId() {
		return teamId;
	}

	public String getTeamName() {
		return teamName;
	}

	public Instant getDateTimeSubmitted() {
		return dateTimeSubmitted;
	}

	public Long getAssignmentId() {
		return activeAssignment.getAssignment().getId();
	}

}