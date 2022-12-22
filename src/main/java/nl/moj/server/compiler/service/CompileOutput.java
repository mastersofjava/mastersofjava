package nl.moj.server.compiler.service;

import java.time.Instant;
import java.util.UUID;

public class CompileOutput {

	private final CompileInput compileInput;
	
	private final String output;
	private final String errorOutput;
	private final int exitvalue;
	private final boolean timedOut;
	
	private final Instant dateTimeStart;
	private final Instant dateTimeEnd;
	
	public CompileOutput(CompileInput compileInput, String output, String errorOutput, int exitvalue, boolean timedOut,  Instant dateTimeStart, Instant dateTimeEnd) {
		this.compileInput = compileInput;
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
		return compileInput.getDateTimeSubmitted();
	}

	public UUID getTeamUuid() {
		return compileInput.getTeamUuid();
	}
	public Long getTeamId() {
		return compileInput.getTeamId();
	}

	public Long getCompetitionSessionId() {
		return compileInput.getCompetitionSessionId();
	}

	public Long getAssignmentId() {
		return compileInput.getAssignmentId();
	}

	public UUID getCompileAttemptUuid() {
		return compileInput.getCompileAttemptUuid();
	}
	
	
}
