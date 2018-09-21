package nl.moj.server;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.CompileService;
import nl.moj.server.runtime.AssignmentRuntime;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.TestService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
@MessageMapping("/submit")
public class SubmitController {

	private CompileService compileService;

	private TestService testService;

	private Executor compiling;

	private Executor testing;

	private Integer timeout;

	private CompetitionRuntime competition;

	private TeamRepository teamRepository;

	private AssignmentRuntime assigmentRuntime;

	public SubmitController(CompileService compileService,
                            TestService testService,
                            @Qualifier("compiling") Executor compiling,
                            @Qualifier("testing") Executor testing,
                            @Value("${moj.server.timeout}") Integer timeout,
                            CompetitionRuntime competition,
                            TeamRepository teamRepository,
                            AssignmentRuntime assignmentRuntime) {
		super();
		this.compileService = compileService;
		this.testService = testService;
		this.compiling = compiling;
		this.testing = testing;
		this.timeout = timeout;
		this.competition = competition;
		this.teamRepository = teamRepository;
		this.assigmentRuntime = assignmentRuntime;
	}

	@MessageMapping("/compile")
	public void compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		message.setTeam(user.getName());
		CompletableFuture.supplyAsync(compileService.compile(message), compiling).orTimeout(timeout, TimeUnit.SECONDS);
	}

	@MessageMapping("/test")
	public void test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		message.setTeam(user.getName());
		CompletableFuture<Void> completableFuture = CompletableFuture
				.supplyAsync(compileService.compileWithTest(message), testing)
				.thenAccept(compileResult -> testService.testAll(compileResult)).whenComplete((value, ex) -> { 
					if (value != null) {
						System.out.println("Result: " + value);
					} else {
						// ... or return an error value:
						System.out.println("Error code: -1. Root cause: " + ex.getCause().getMessage());
					}
				});
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
		AssignmentState state = competition.getAssignmentState();
        if (!assigmentRuntime.isTeamFinished(team) && assigmentRuntime.hasResubmits(team.getName())) {
			long scoreAtSubmissionTime = state.getTimeRemaining();
			message.setTeam(user.getName());
			message.setScoreAtSubmissionTime(scoreAtSubmissionTime);
			CompletableFuture.supplyAsync(compileService.compileForSubmit(message), testing)
					.orTimeout(timeout, TimeUnit.SECONDS)
					.thenComposeAsync(compileResult -> testService.testSubmit(compileResult), testing);
		} else {
			log.warn("Team {} tried to submit but is already finished", user.getName());
		}
	}

	@JsonDeserialize(using = SourceMessageDeserializer.class)
	@Data
	public static class SourceMessage {

		private String team;
		private Map<String, String> source;
		private List<String> tests;
		private Long scoreAtSubmissionTime;

		public SourceMessage(String team, Map<String, String> source, List<String> tests,
				Long scoreAtSubmissionTime) {
			this.team = team;
			this.source = source;
			this.tests = tests;
			this.scoreAtSubmissionTime = scoreAtSubmissionTime;
		}

		public SourceMessage(Map<String, String> source, List<String> tests) {
			this.source = source;
			this.tests = tests;
		}
	}

	public static class SourceMessageDeserializer extends JsonDeserializer<SourceMessage> {

		public SourceMessageDeserializer() {
		}

		@Override
		public SourceMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
				throws IOException {
			JsonNode node = jsonParser.getCodec().readTree(jsonParser);
			Map<String, String> sources = new HashMap<>();
			if (node.get("sources") != null && node.get("sources").isArray()) {
				ArrayNode sourceArray = (ArrayNode) node.get("sources");
				for (int i = 0; i < sourceArray.size(); i++) {
					JsonNode sourceElement = sourceArray.get(i);
					sources.put(sourceElement.get("filename").textValue(), sourceElement.get("content").textValue());
				}
			}
			List<String> tests = new ArrayList<>();
			if (node.get("tests") != null && node.get("tests").isArray()) {
				ArrayNode jsonTests = (ArrayNode) node.get("tests");
				jsonTests.forEach(t -> tests.add(t.asText()));
			}
			return new SourceMessage(sources, tests);
		}
	}
}
