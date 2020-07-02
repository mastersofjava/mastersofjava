package nl.moj.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;

import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.login.SignupForm;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;

@ExtendWith(MockitoExtension.class)
class IndexControllerTest {

	@Mock
	private CompetitionRuntime competition;
    @Mock 
    private TeamRepository teamRepository;
    @Mock 
    private TeamService teamService;
    @Mock 
    private AssignmentStatusRepository assignmentStatusRepository;
    @Mock 
    private AssignmentRepository assignmentRepository;
    @Mock 
    private CompetitionService competitionService;
	
    @InjectMocks
    private IndexController indexController;
    
	@Test
	void testIndexNoUser() {
		assertEquals("redirect:/sso/login", indexController.index(null, null, null));
	}

	@Test
	void testIndexNewTeam() {
		GrantedAuthority grantedAuthority = Mockito.mock(GrantedAuthority.class);
		when(grantedAuthority.getAuthority()).thenReturn("ROLE_USER");
		Collection<GrantedAuthority> grantedAuthorities = List.of(grantedAuthority);
		
		org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken user = Mockito.mock(org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken.class);
		when(user.getName()).thenReturn("userName");
		when(user.getAuthorities()).thenReturn(grantedAuthorities);
		when(teamRepository.findByName(Mockito.anyString())).thenReturn(null);
		
		assertEquals("index", indexController.index(Mockito.mock(Model.class), user, null));

		Mockito.verify(competitionService).createNewTeam(Mockito.any(SignupForm.class), Mockito.eq(Role.USER));
	}
	
	@Test
	void testIndexExistingTeam() {
		GrantedAuthority grantedAuthority = Mockito.mock(GrantedAuthority.class);
		when(grantedAuthority.getAuthority()).thenReturn("ROLE_USER");
		Collection<GrantedAuthority> grantedAuthorities = List.of(grantedAuthority);
		
		org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken user = Mockito.mock(org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken.class);
		when(user.getName()).thenReturn("userName");
		when(user.getAuthorities()).thenReturn(grantedAuthorities);
		
		Team team = Team.builder()
				.name("userName")
				.role(Role.USER)
				.build();
		when(teamRepository.findByName(Mockito.anyString())).thenReturn(team );
		
		assertEquals("index", indexController.index(Mockito.mock(Model.class), user, null));

		Mockito.verify(competitionService, never()).createNewTeam(Mockito.any(SignupForm.class), Mockito.eq(Role.USER));
	}
}
