package nl.moj.server;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

import nl.moj.server.competition.ScoreService;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.CompileService;
import nl.moj.server.test.TestResult;
import nl.moj.server.test.TestService;

@Controller
@MessageMapping("/submit")
public class SubmitController {

	private static final Logger log = LoggerFactory.getLogger(SubmitController.class);

	@Autowired
	private CompileService compileService;

	@Autowired
	private TestService testService;

	@Autowired
	@Qualifier("compiling")
	private Executor compiling;

	@Autowired
	@Qualifier("testing")
	private Executor testing;

	@Autowired
	private SimpMessagingTemplate template;

	@Autowired
	private ScoreService scoreService;
	
	@Value("${moj.server.timeout}")
	private int TIMEOUT;

	@MessageMapping("/compile")
	public void compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		CompletableFuture.supplyAsync(compileService.compile(message.getSource(), user.getName()), compiling)
				.orTimeout(1, TimeUnit.SECONDS).thenAccept(compileResult -> sendFeedbackMessage(compileResult));
	}

	@MessageMapping("/test")
	public void test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		CompletableFuture.supplyAsync(compileService.compileWithTest(message.getSource(), user.getName()), testing)
				.orTimeout(TIMEOUT, TimeUnit.SECONDS)
				.thenComposeAsync(compileResult -> testService.testAll(compileResult), testing);
				//.orTimeout(TIMEOUT, TimeUnit.SECONDS)
				//.thenAccept(testResult -> {
				//	applyTestPenalty(testResult);
				//});
	}

	@MessageMapping("/submit")
	public void submit(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		CompletableFuture.supplyAsync(compileService.compileForSubmit(message.getSource(), user.getName()), testing)
				.orTimeout(TIMEOUT, TimeUnit.SECONDS)
				.thenComposeAsync(compileResult -> testService.testSubmit(compileResult), testing)
				//.orTimeout(TIMEOUT, TimeUnit.SECONDS)
				.thenAccept(testResult -> {
					setFinalAssignmentScore(testResult);
				});
	}

	private void sendFeedbackMessage(CompileResult compileResult) {
		log.info("sending compileResult feedback");
		template.convertAndSendToUser(compileResult.getUser(), "/queue/compilefeedback",
				new FeedbackMessage(compileResult.getUser(), compileResult.getCompileResult(), compileResult.isSuccessful()));
	}


	private void applyTestPenalty(TestResult testResult) {
		if (testResult.isSuccessful()) {
			scoreService.applyTestPenaltyOrCredit(testResult.getUser());
			template.convertAndSend("/queue/rankings", "refresh");
		}

	}

	private void setFinalAssignmentScore(TestResult testResult) {
		if (testResult.isSuccessful()) {
			scoreService.subtractSpentSeconds(testResult.getUser());
			log.info("refreshScoreBoard ");
			template.convertAndSend("/queue/rankings", "refresh");
		}
	}

	@JsonDeserialize(using = SourceMessageDeserializer.class)
	public static class SourceMessage {

		private String team;
		private Map<String, String> source;
		
		public SourceMessage() {
		}

		public SourceMessage(String team, Map<String, String> source) {
			this.team = team;
			this.source = source;
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

	}

	public class FeedbackMessage {

		private String team;
		private String text;
		private boolean succuess;

		public FeedbackMessage(String team, String text, boolean succuess) {
			super();
			this.team = team;
			this.text = text;
			this.setSuccuess(succuess);
		}

		public String getTeam() {
			return team;
		}

		public void setTeam(String team) {
			this.team = team;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public boolean isSuccuess() {
			return succuess;
		}

		public void setSuccuess(boolean succuess) {
			this.succuess = succuess;
		}

	}

	public class SourceMessageDeserializer extends JsonDeserializer<SourceMessage> {
		@Override
		public SourceMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
				throws IOException {
			JsonNode node = jsonParser.getCodec().readTree(jsonParser);
			String team = node.get("team").textValue();
			Map<String, String> sources = new HashMap<>();
			if (node.get("source").isArray()) {
				ArrayNode sourceArray = (ArrayNode) node.get("source");
				for (int i = 0; i < sourceArray.size(); i++) {
					JsonNode sourceElement = sourceArray.get(i);
					sources.put(sourceElement.get("filename").textValue(), sourceElement.get("content").textValue());
				}
			}
			return new SubmitController.SourceMessage(team, sources);
		}
	}

}
