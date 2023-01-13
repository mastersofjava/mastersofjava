package nl.moj.worker.java.compile;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class CompileOutput {

    private UUID runId;
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
