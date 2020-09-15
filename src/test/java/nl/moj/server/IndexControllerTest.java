package nl.moj.server;

import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.teams.model.Team;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
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
		org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken user = Mockito.mock(org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken.class);

		when(userService.createOrUpdate(user)).thenReturn(new User());
		assertEquals("createteam", indexController.index(Mockito.mock(Model.class), user, null));
	}

	@Test
	void testIndexExistingTeam() {
		org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken userToken = Mockito.mock(org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken.class);
		CompetitionSession session = createGameSession();

		List<CompetitionSession> sessions = new ArrayList<>();
		sessions.add(session);
		User user = new User();
		user.setTeam(Team.builder().name("test").build());
		when(userService.createOrUpdate(userToken)).thenReturn(user);
		when(competition.getCompetitionSession()).thenReturn(session);
		when(competitionSessionRepository.findAll()).thenReturn(sessions);
		assertEquals("play", indexController.index(Mockito.mock(Model.class), userToken, null));

		Mockito.verify(competitionService, never()).addTeam(Mockito.any(Team.class));
	}
}
