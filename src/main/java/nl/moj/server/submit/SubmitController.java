package nl.moj.server.submit;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.CompileService;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.TestService;
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
public class SubmitController {

	private CompileService compileService;

	private TestService testService;

	private SubmitService submitService;

	private Executor compiling;

	private MojServerProperties mojServerProperties;

	private CompetitionRuntime competition;

	private TeamRepository teamRepository;

	public SubmitController(CompileService compileService, TestService testService,
							@Qualifier("compiling") Executor compiling,
							MojServerProperties mojServerProperties, CompetitionRuntime competition,
							TeamRepository teamRepository, SubmitService submitService) {
		super();
		this.compileService = compileService;
		this.testService = testService;
		this.compiling = compiling;
		this.mojServerProperties = mojServerProperties;
		this.competition = competition;
		this.teamRepository = teamRepository;
		this.submitService = submitService;
	}

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
