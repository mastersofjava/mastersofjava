package nl.moj.worker.compile.service;

import java.time.Instant;
import java.util.UUID;

public class CompileOutput {

	private final String output;
	private final String errorOutput;
	private final int exitvalue;
	private final boolean timedOut;
	
	private final Instant dateTimeStart;
	private final Instant dateTimeEnd;
	
	public CompileOutput(String output, String errorOutput, int exitvalue, boolean timedOut,  Instant dateTimeStart, Instant dateTimeEnd) {
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
}
