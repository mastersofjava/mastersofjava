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
package nl.moj.server.teams.model;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public final class Role {
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String GAME_MASTER = "ROLE_GAME_MASTER";
    public static final String USER = "ROLE_USER";
    public static final String ANONYMOUS = "ROLE_ANONYMOUS";
    
	public static boolean isWithControleRole(List<String> roles) {
		return roles.contains(Role.ADMIN) || roles.contains(Role.GAME_MASTER);
	}
	
	public static boolean isWithControleRole(KeycloakAuthenticationToken user) {
		if (user==null || user.getAuthorities()==null || user.getAuthorities().isEmpty()) {
			return false;
		}
		return isWithControleRole(user
    			.getAuthorities().stream()
    			.map(GrantedAuthority::getAuthority)
    			.collect(Collectors.toList()));
	}
}
