package nl.moj.server.test.service;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TestResult {

    private UUID testCaseUuid;
    private Instant dateTimeStart;
    private Instant dateTimeEnd;
    private String testName;
    private String testOutput;
    private boolean success;

    @Builder.Default
    private boolean timeout = false;
}
