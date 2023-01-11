package nl.moj.server.submit.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SubmitResponse {

    UUID getAttempt();

    TestResponse getTestResponse();

    boolean isAborted();

    String getReason();

    Instant getStarted();

    Instant getEnded();
}