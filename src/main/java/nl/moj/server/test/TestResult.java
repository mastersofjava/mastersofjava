package nl.moj.server.test;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.teams.model.Team;

@Builder
@Data
public class TestResult {
	private Team team;
	private String message;
	private boolean successful;
	private String testName;
}
