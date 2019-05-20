package nl.moj.server.test.service;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TestResults {

    private List<TestResult> results;
    private UUID testAttemptUuid;

    public boolean isSuccess() {
        return results.stream().allMatch(TestResult::isSuccess);
    }
}
