package nl.moj.server;

import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

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
	@Mock
	private CompetitionSessionRepository competitionSessionRepository;
	@Mock
	private UserService userService;

	@InjectMocks
	private GameController indexController;



	private CompetitionSession createGameSession() {
		CompetitionSession session = new CompetitionSession();
		session.setUuid(UUID.randomUUID());
		session.setId(1L);
		return session;
	}

	@Test
	void testIndexNewTeam() {
		GrantedAuthority grantedAuthority = Mockito.mock(GrantedAuthority.class);
		//  when(grantedAuthority.getAuthority()).thenReturn("ROLE_USER");
		Collection<GrantedAuthority> grantedAuthorities = List.of(grantedAuthority);

		org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken user = Mockito.mock(org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken.class);

		// when(competition.getCompetitionSession()).thenReturn(createGameSession());
		//when(user.getName()).thenReturn("userName");
		// when(user.getAuthorities()).thenReturn(grantedAuthorities);
		//when(teamRepository.findByName(Mockito.anyString())).thenReturn(null);
		when(userService.createOrUpdate(user)).thenReturn(new User());
		assertEquals("createteam", indexController.index(Mockito.mock(Model.class), user, null));

		// Mockito.verify(competitionService).addTeam(Mockito.any(Team.class));
	}

	@Test
	void testIndexExistingTeam() {
		GrantedAuthority grantedAuthority = Mockito.mock(GrantedAuthority.class);
		//when(grantedAuthority.getAuthority()).thenReturn("ROLE_USER");
		Collection<GrantedAuthority> grantedAuthorities = List.of(grantedAuthority);

		org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken userToken = Mockito.mock(org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken.class);
		User user = new User();
		user.setTeam(Team.builder().name("test").build());
		when(userService.createOrUpdate(userToken)).thenReturn(user);
		when(competition.getCompetitionSession()).thenReturn(createGameSession());
		// when(user.getName()).thenReturn("userName");

		Team team = Team.builder()
				.name("userName")
				.build();
//        when(teamRepository.findByName(Mockito.anyString())).thenReturn(team);

		assertEquals("play", indexController.index(Mockito.mock(Model.class), userToken, null));

		Mockito.verify(competitionService, never()).addTeam(Mockito.any(Team.class));
	}
}
