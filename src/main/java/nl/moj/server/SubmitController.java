package nl.moj.server;

import java.io.IOException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import net.tascalate.concurrent.CompletableTask;
import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.CompileService;
import nl.moj.server.persistence.ResultMapper;
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
	@Qualifier("timed")
	private Executor timed;

	@Autowired
	private SimpMessagingTemplate template;

	@Autowired
	private ResultMapper resultMapper;

	@Autowired
	private Competition competition;

	@MessageMapping("/compile")
	public void compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		CompletableTask.supplyAsync(compileService.compile(message.getSource(), user.getName()), timed)
				.thenAccept(testResult -> sendFeedbackMessage(testResult)).get();
	}

	@MessageMapping("/test")
	public void test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		message.getSource().forEach((k, v) -> log.info("{},{}", k, v));
		CompletableTask.supplyAsync(compileService.compile(message.getSource(), user.getName(), true), timed)
				.thenComposeAsync(compileResult -> testService.test(compileResult), timed)
				.thenAccept(testResult -> sendFeedbackMessage(testResult)).get();
	}

	@MessageMapping("/submit")
	public void submit(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		CompletableTask.supplyAsync(compileService.compile(message.getSource(), user.getName(), true), timed)
		.thenComposeAsync(compileResult -> testService.test(compileResult), timed)
				.thenAccept(testResult -> {
					updateScoreBoard(testResult);
					sendFeedbackMessage(testResult);
				}).get();
	}

	private void sendFeedbackMessage(CompileResult compileResult) {
		log.info("sending compileResult feedback");
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		template.convertAndSendToUser(compileResult.getUser(), "/queue/feedback",
				new FeedbackMessage(compileResult.getUser(), compileResult.getCompileResult(), time));
	}

	private void sendFeedbackMessage(TestResult testResult) {
		log.info("sending testResult feedback");
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		template.convertAndSendToUser(testResult.getUser(), "/queue/feedback",
				new FeedbackMessage(testResult.getUser(), testResult.getTestResult(), time));
	}

	private void updateScoreBoard(TestResult testResult) {
		if (testResult.isSuccessful()) {
			int solutiontime = competition.getCurrentAssignment().getSolutionTime();
			int seconds =  competition.getSecondsElapsed();
			int newscore = solutiontime - seconds;
			String teamname = testResult.getUser();
			String assignment = competition.getCurrentAssignment().getName();
			Integer score = resultMapper.getScore(teamname, assignment);
			if (score == null)
				score = 0;
			resultMapper.updateScore(teamname, assignment, score + newscore);
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
		private String time;

		public FeedbackMessage(String team, String text, String time) {
			super();
			this.team = team;
			this.text = text;
			this.time = time;
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

		public String getTime() {
			return time;
		}

		public void setTime(String time) {
			this.time = time;
		}
	}
	
	public class SourceMessageDeserializer extends JsonDeserializer<SourceMessage> {
	    @Override
	    public SourceMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
	        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
	        String team = node.get("team").textValue();
	        Map<String,String> sources = new HashMap<>();
	        if (node.get("source").isArray()) {
	        	ArrayNode sourceArray = (ArrayNode)node.get("source");
	        	for (int i =0; i < sourceArray.size(); i++) {
	        		JsonNode sourceElement = sourceArray.get(i);
	        		sources.put(sourceElement.get("filename").textValue(), sourceElement.get("content").textValue());
	        	}
	        }
	        return new SubmitController.SourceMessage(team,sources);
	    }
	}

}


