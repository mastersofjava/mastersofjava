package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.teams.model.Team;

@Builder(toBuilder = true)
@Getter
public class TeamStatus {

	private Team team;

	@Builder.Default
	private Long submitTime = 0L;

	@Builder.Default
	private Long score = 0L;

	@Builder.Default
	private Integer submits = 0;

	@Builder.Default
	private boolean completed = false;

	public static TeamStatus init(Team team) {
		return TeamStatus.builder()
				.completed(false)
				.score(0L)
				.submitTime(0L)
				.team(team)
				.submits(0)
				.build();
	}
}
