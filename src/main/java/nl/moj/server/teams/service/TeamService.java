package nl.moj.server.teams.service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final MojServerProperties mojServerProperties;
	private final TeamRepository teamRepository;

	public Path getTeamDirectory(Team team) {
		return mojServerProperties.getDirectories().getBaseDirectory().resolve(
				mojServerProperties.getDirectories().getTeamDirectory()).resolve(team.getName());
	}

	public List<Team> getTeams() {
		return teamRepository.findAllByRole(Role.ROLE_USER);
	}

}
