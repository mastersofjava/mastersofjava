package nl.moj.server.feedback.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.moj.server.teams.model.Team;

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
public class TeamFeedback {

    private Team team;

    @Builder.Default
    private Map<String, Boolean> testResults = new HashMap<>();

    @Builder.Default
    private boolean submitted = false;

    @Builder.Default
    private boolean success = false;

    public boolean isTestRun(String test) {
        return testResults.containsKey(test);
    }

    public boolean isTestSuccess(String test) {
        if (isTestRun(test)) {
            return testResults.get(test);
        }
        return false;
    }
}
