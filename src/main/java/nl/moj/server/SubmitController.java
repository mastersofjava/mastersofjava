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
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.TestService;
import org.springframework.beans.factory.annotation.Qualifier;
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
@MessageMapping("/submit")
@Slf4j
public class SubmitController {

	private CompileService compileService;

	private TestService testService;

	private Executor compiling;

	private Executor testing;

	private MojServerProperties mojServerProperties;

	private CompetitionRuntime competition;

	private TeamRepository teamRepository;

	public SubmitController(CompileService compileService, TestService testService,
							@Qualifier("compiling") Executor compiling, @Qualifier("testing") Executor testing,
							MojServerProperties mojServerProperties, CompetitionRuntime competition, TeamRepository teamRepository) {
		super();
		this.compileService = compileService;
		this.testService = testService;
		this.compiling = compiling;
		this.testing = testing;
		this.mojServerProperties = mojServerProperties;
		this.competition = competition;
		this.teamRepository = teamRepository;
	}

	@MessageMapping("/compile")
	public void compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		message.setTeam(user.getName());
		CompletableFuture.supplyAsync(compileService.compile(message), compiling)
				.orTimeout(mojServerProperties.getRuntimes().getCompile().getTimeout(), TimeUnit.SECONDS);
	}

	@MessageMapping("/test")
	public void test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		message.setTeam(user.getName());
		CompletableFuture
				.supplyAsync(compileService.compileWithTest(message), compiling)
				.thenCompose(compileResult -> testService.testAll(compileResult))
				.orTimeout(mojServerProperties.getRuntimes().getTest().getTimeout(), TimeUnit.SECONDS)
				.whenComplete((testResults, error) -> {
					if( error != null ) {
						log.error("Testing failed: {}", error.getMessage(), error);
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
		// TODO we need to handle resubmits
		Team team = teamRepository.findByName(user.getName());
		AssignmentState state = competition.getAssignmentState();
		if (!state.isTeamFinished(team)) {
			long scoreAtSubmissionTime = state.getTimeRemaining();
			message.setTeam(user.getName());
			message.setScoreAtSubmissionTime(scoreAtSubmissionTime);
			CompletableFuture.supplyAsync(compileService.compileForSubmit(message), compiling)
					.thenCompose(compileResult -> testService.testSubmit(compileResult))
					.orTimeout(mojServerProperties.getRuntimes().getTest().getTimeout(), TimeUnit.SECONDS)
					.whenComplete( (testResult,error) -> {
						if( error != null ) {
							log.error("Testing failed: {}", error.getMessage(), error);
						}
						if( testResult != null ) {
							log.debug("Test result: {}", testResult.isSuccessful());
						}
					} );
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
