package nl.moj.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.model.Team;
import nl.moj.server.repository.TeamRepository;

@Service
@RequiredArgsConstructor
public class TeamDetailsService implements UserDetailsService {

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
		SimpleGrantedAuthority sGA = new SimpleGrantedAuthority(team.getRole());
		authList.add(sGA);
		return authList;
	}
}
