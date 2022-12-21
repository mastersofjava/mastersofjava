package nl.moj.server.compiler.service;

import java.time.Instant;
import java.util.UUID;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.runtime.model.ActiveAssignment;
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
	private final SourceMessage sourceMessage;
	
	private final AssignmentDescriptor assignmentDescriptor;
	private final String assignmentName;
	private final UUID assignmentUuid;
	private final Long assignmentId;
	private final UUID competitionSessionUuid;
	private final Long competitionSessionId;

	CompileInputWrapper(CompileRequest compileRequest, ActiveAssignment activeAssignment) {
		this.teamUuid = compileRequest.getTeam().getUuid();
		this.teamName = compileRequest.getTeam().getName();
		this.teamId = compileRequest.getTeam().getId();
		this.sourceMessage = compileRequest.getSourceMessage();
		this.compileAttemptUuid = UUID.randomUUID();
		this.dateTimeSubmitted = compileRequest.getDateTimeSubmitted();
		this.assignmentDescriptor = activeAssignment.getAssignmentDescriptor();
		this.assignmentName = activeAssignment.getAssignment().getName();
		this.assignmentUuid = activeAssignment.getAssignment().getUuid();
		this.assignmentId = activeAssignment.getAssignment().getId();
		this.competitionSessionUuid = activeAssignment.getCompetitionSession().getUuid();
		this.competitionSessionId = activeAssignment.getCompetitionSession().getId();
    }


	public UUID getCompileAttemptUuid() {
		return compileAttemptUuid;
	}

	public AssignmentDescriptor getAssignmentDescriptor() {
		return assignmentDescriptor;
	}
	
	public String getAssignmentName() {
		return assignmentName;
	}

	public UUID getCompetitionSessionUuid() {
		return competitionSessionUuid;
	}
	
	public Long getCompetitionSessionId() {
		return competitionSessionId;
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
		return assignmentId;
	}

	public UUID getAssignmentUuid() {
		return assignmentUuid;
	}

}