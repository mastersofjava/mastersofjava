package nl.moj.server;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

import nl.moj.server.runtime.Competition;
import nl.moj.server.compile.CompileService;
import nl.moj.server.test.TestService;

@Controller
@MessageMapping("/submit")
public class SubmitController {

	private CompileService compileService;

	private TestService testService;

	private Executor compiling;

	private Executor testing;

	private Integer timeout;

	private Competition competition;

	public SubmitController(CompileService compileService, TestService testService,
			@Qualifier("compiling") Executor compiling, @Qualifier("testing") Executor testing,
			@Value("${moj.server.timeout}") Integer timeout, Competition competition) {
		super();
		this.compileService = compileService;
		this.testService = testService;
		this.compiling = compiling;
		this.testing = testing;
		this.timeout = timeout;
		this.competition = competition;
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
		if (!competition.getCurrentAssignment().isTeamFinished(user.getName())) {
			int scoreAtSubmissionTime = competition.getRemainingTime();
			message.setTeam(user.getName());
			message.setScoreAtSubmissionTime(scoreAtSubmissionTime);
			CompletableFuture.supplyAsync(compileService.compileForSubmit(message), testing)
					.orTimeout(timeout, TimeUnit.SECONDS)
					.thenComposeAsync(compileResult -> testService.testSubmit(compileResult), testing);
		}
	}

	@JsonDeserialize(using = SourceMessageDeserializer.class)
	public static class SourceMessage {

		private String team;
		private Map<String, String> source;
		private List<String> tests;
		private Integer scoreAtSubmissionTime;

		public SourceMessage(String team, Map<String, String> source, List<String> tests,
				Integer scoreAtSubmissionTime) {
			this.team = team;
			this.source = source;
			this.tests = tests;
			this.scoreAtSubmissionTime = scoreAtSubmissionTime;
		}

		public SourceMessage(Map<String, String> source, List<String> tests) {
			this.source = source;
			this.tests = tests;
		}

		public String getTeam() {
			return team;
		}

		public void setTeam(String team) {
			this.team = team;
		}

		public Map<String, String> getSource() {
			return source;
		}

		public void setSource(Map<String, String> source) {
			this.source = source;
		}

		public List<String> getTests() {
			return tests;
		}

		public void setTests(List<String> tests) {
			this.tests = tests;
		}

		public Integer getScoreAtSubmissionTime() {
			return scoreAtSubmissionTime;
		}

		public void setScoreAtSubmissionTime(Integer scoreAtSubmissionTime) {
			this.scoreAtSubmissionTime = scoreAtSubmissionTime;
		}
	}

	private class SourceMessageDeserializer extends JsonDeserializer<SourceMessage> {
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
