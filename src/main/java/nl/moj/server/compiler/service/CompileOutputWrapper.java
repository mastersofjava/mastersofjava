package nl.moj.server.compiler.service;

import java.time.Instant;
import java.util.UUID;

public class CompileOutputWrapper {

	private final CompileInputWrapper compileInputWrapper;
	
	private final String output;
	private final String errorOutput;
	private final int exitvalue;
	private final boolean timedOut;
	
	private final Instant dateTimeStart;
	private final Instant dateTimeEnd;
	
	public CompileOutputWrapper(CompileInputWrapper compileInputWrapper, String output, String errorOutput, int exitvalue, boolean timedOut,  Instant dateTimeStart, Instant dateTimeEnd) {
		this.compileInputWrapper = compileInputWrapper;
		this.output = output;
		this.errorOutput = errorOutput;
		this.exitvalue = exitvalue;
		this.timedOut = timedOut;
		this.dateTimeStart = dateTimeStart;
		this.dateTimeEnd = dateTimeEnd;
	}

	public int getExitvalue() {
		return exitvalue;
	}
	public boolean isTimedOut() {
		return timedOut;
	}

	public String getOutput() {
		return output;
	}

	public String getErrorOutput() {
		return errorOutput;
	}

	public Instant getDateTimeStart() {
		return dateTimeStart;
	}

	public Instant getDateTimeEnd() {
		return dateTimeEnd;
	}

	public Instant getDateSubmitted() {
		return compileInputWrapper.getDateTimeSubmitted();
	}

	public UUID getTeamUuid() {
		return compileInputWrapper.getTeamUuid();
	}
	public Long getTeamId() {
		return compileInputWrapper.getTeamId();
	}

	public Long getCompetitionSessionId() {
		return compileInputWrapper.getCompetitionSessionId();
	}

	public Long getAssignmentId() {
		return compileInputWrapper.getAssignmentId();
	}

	public UUID getCompileAttemptUuid() {
		return compileInputWrapper.getCompileAttemptUuid();
	}
	
	
}
