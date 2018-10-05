package nl.moj.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamDetailsService implements UserDetailsService {

	private final MojServerProperties mojServerProperties;
    private final TeamRepository teamRepository;

	@Override
	public UserDetails loadUserByUsername(String teamname) throws UsernameNotFoundException {
		Team team = teamRepository.findByName(teamname);
		if (team == null)
			throw new UsernameNotFoundException("no team found with name: " + teamname);
		return new User(teamname, team.getPassword(), true, true, true, true, getAuthorities(team));
	}

	private Collection<? extends GrantedAuthority> getAuthorities(Team team) {
		List<GrantedAuthority> authList = new ArrayList<>();
		SimpleGrantedAuthority sGA = new SimpleGrantedAuthority(team.getRole().toString());
		authList.add(sGA);
		return authList;
	}

	public void initTeam(String name) {
		Path teamdir = mojServerProperties.getDirectories().getBaseDirectory().resolve(
				mojServerProperties.getDirectories().getTeamDirectory()).resolve(name);
		if (!Files.exists(teamdir)) {
			try {
				Files.createDirectories(teamdir);
			} catch (IOException e) {
				log.error("error while creating team directory", e);
			}
		}
	}
}
