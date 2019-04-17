package nl.moj.server.test.service;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.teams.model.Team;

import java.util.UUID;

@Builder
@Data
public class TestResult {

	private UUID testCaseUuid;

	private String testName;
	private String testOutput;
	private boolean success;

	@Builder.Default
	private boolean timeout = false;
}
