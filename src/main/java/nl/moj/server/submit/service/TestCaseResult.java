package nl.moj.server.submit.service;

import java.time.Instant;
import java.util.UUID;

public interface TestCaseResult {

    UUID getTestCase();

    Instant getStarted();

    Instant getEnded();

    boolean isSuccess();

    boolean isTimeout();

    String getOutput();

    // this should not be needed it is directly linked to the UI
    String getName();
}
