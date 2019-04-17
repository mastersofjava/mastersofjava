package nl.moj.server.submit;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.service.TestResult;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Getter
public class SubmitResult {

    @Builder.Default
    private final int remainingSubmits = 0;

    @Builder.Default
    private final long score = 0L;

    private CompileResult compileResult;

    @Builder.Default
    private List<TestResult> testResults = new ArrayList<>();

    @Builder.Default
    private boolean success = false;
}
