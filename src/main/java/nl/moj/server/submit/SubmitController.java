package nl.moj.server.submit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.service.TestService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.concurrent.Executor;

@Controller
@MessageMapping("/submit")
@Slf4j
@AllArgsConstructor
public class SubmitController {

	private SubmitService submitService;

	private TeamRepository teamRepository;

	@MessageMapping("/compile")
	public void compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		Team team = teamRepository.findByName(user.getName());
		submitService.compile(team,message);
	}

	@MessageMapping("/test")
	public void test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		Team team = teamRepository.findByName(user.getName());
		submitService.test(team,message);

	}

	/**
	 * Submits the final solution of the team and closes the assignment for the
	 * submitting team. The submitting team cannot work with the assignment after
	 * closing.
	 *
	 * @param message
	 * @param user
	 * @param mesg
	 * @throws Exception
	 */
	@MessageMapping("/submit")
	public void submit(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		Team team = teamRepository.findByName(user.getName());
		submitService.submit(team,message);
	}
}
