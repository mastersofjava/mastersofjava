package nl.moj.server.submit;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.test.service.TestResults;

@Builder(toBuilder = true)
@Getter
public class SubmitResult {

    private final UUID team;

    private final Instant dateTimeStart;

    private final Instant dateTimeEnd;

    @Builder.Default
    private final int remainingSubmits = 0;

    @Builder.Default
    private final long score = 0L;

    private CompileResult compileResult;

    private TestResults testResults;

    @Builder.Default
    private boolean success = false;
}
