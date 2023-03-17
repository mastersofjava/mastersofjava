package nl.moj.worker.java.compile;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class CompileOutput {

    private String output;
    private String errorOutput;
    @Builder.Default
    private boolean success = false;
    @Builder.Default
    private boolean timedOut = false;
    @Builder.Default
    private boolean aborted = false;
    private String reason;

    private Instant dateTimeStart;
    private Instant dateTimeEnd;
}
