/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamDetailsService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";
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
        SimpleGrantedAuthority sGA = new SimpleGrantedAuthority(extractRole(team));
        authList.add(sGA);
        return authList;
    }

    private String extractRole(Team team) {
        if (team.getRole() != null) {
            String role = team.getRole();
            if (!role.startsWith(ROLE_PREFIX)) {
                role = ROLE_PREFIX + role;
            }
            return role;
        }
        return ROLE_PREFIX + Role.ANONYMOUS;
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
