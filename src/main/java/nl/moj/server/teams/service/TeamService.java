package nl.moj.server.teams.service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final MojServerProperties mojServerProperties;
	private final TeamRepository teamRepository;

	public Path getTeamDirectory(Team team) {
		return Paths.get(mojServerProperties.getDirectories().getBaseDirectory(),
				mojServerProperties.getDirectories().getTeamDirectory(), team.getName());
	}

	public List<Team> getTeams() {
		return teamRepository.findAll();
	}

}
