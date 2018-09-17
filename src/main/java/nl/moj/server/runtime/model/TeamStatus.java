package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.teams.model.Team;

@Builder
@Getter
public class TeamStatus {

	private Team team;

	@Builder.Default
	private Long submitTime = 0L;

	@Builder.Default
	private Long score = 0L;
}
