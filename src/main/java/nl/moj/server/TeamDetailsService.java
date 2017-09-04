package nl.moj.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import nl.moj.server.model.Team;
import nl.moj.server.persistence.TeamMapper;

@Service
public class TeamDetailsService implements UserDetailsService {
	@Autowired
	private TeamMapper teamMapper;

	@Override
	public UserDetails loadUserByUsername(String teamname) throws UsernameNotFoundException {
		Team team = teamMapper.findByName(teamname);
		if (team == null)
			throw new UsernameNotFoundException("no team found with name: " + teamname);
		return new User(teamname, team.getPassword(), true, true, true, true, getAuthorities(team));
	}

	public Collection<? extends GrantedAuthority> getAuthorities(Team team) {
		List<GrantedAuthority> authList = new ArrayList<>();
		SimpleGrantedAuthority sGA = new SimpleGrantedAuthority(team.getRole());
		authList.add(sGA);
		return authList;
	}
}
