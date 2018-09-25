package nl.moj.server.compiler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.moj.server.teams.model.Team;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CompileResult {

	private Team team;
	private boolean successful;
	private String message;

	public static final CompileResult success(Team team) {
		return new CompileResult(team,true,"Compiled successfully.");
	}

	public static final CompileResult fail(Team team, String message) {
		return new CompileResult(team,false,message);
	}

}
