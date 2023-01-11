package nl.moj.server.submit.service;

import java.time.Instant;
import java.util.UUID;

public interface CompileResponse {

    UUID getAttempt();

    boolean isAborted();

    String getReason();

    String getOutput();

    boolean isSuccess();

    boolean isTimeout();

    Instant getStarted();

    Instant getEnded();
}
