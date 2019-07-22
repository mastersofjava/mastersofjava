package nl.moj.server.submit;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.service.TestResult;
import nl.moj.server.test.service.TestResults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
