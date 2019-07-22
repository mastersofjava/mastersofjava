package nl.moj.server.test.service;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TestResults {

    private UUID testAttemptUuid;
    private Instant dateTimeStart;
    private Instant dateTimeEnd;
    private List<TestResult> results;

    public boolean isSuccess() {
        return results.stream().allMatch(TestResult::isSuccess);
    }
}
