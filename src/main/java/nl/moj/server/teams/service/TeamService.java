package nl.moj.server.teams.service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.DirectoriesConfiguration;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final DirectoriesConfiguration directoriesConfiguration;
	private final TeamRepository teamRepository;

	public Path getTeamDirectory(Team team) {
		return Paths.get(directoriesConfiguration.getBaseDirectory(), directoriesConfiguration.getTeamDirectory(), team.getName());
	}

	public List<Team> getTeams() {
		return teamRepository.findAll();
	}

}
