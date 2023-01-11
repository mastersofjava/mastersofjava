package nl.moj.server.submit.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import nl.moj.server.test.service.TestCaseOutput;

public interface TestResponse {

    UUID getAttempt();

    CompileResponse getCompileResponse();

    boolean isAborted();

    String getReason();

    Instant getStarted();

    Instant getEnded();

    List<? extends TestCaseResult> getTestCaseResults();
}
