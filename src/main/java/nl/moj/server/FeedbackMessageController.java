package nl.moj.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import nl.moj.server.TaskControlController.TaskMessage;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.test.TestResult;

@Controller
public class FeedbackMessageController {
	
	private static final Logger log = LoggerFactory.getLogger(FeedbackMessageController.class);

	private SimpMessagingTemplate template;
	
	public FeedbackMessageController(SimpMessagingTemplate template) {
		super();
		this.template = template;
	}

	public void sendTestFeedbackMessage(TestResult testResult, Boolean submit, Integer score) {
		log.info("sending testResult feedback");
		template.convertAndSendToUser(testResult.getUser(), "/queue/feedback", new TestFeedbackMessage(
				testResult.getUser(), testResult.getTestname(), testResult.getResult(), testResult.isSuccessful(), submit, score));
		template.convertAndSend("/queue/feedbackpage", new TestFeedbackMessage(testResult.getUser(),
				testResult.getTestname(), null, testResult.isSuccessful(), submit, score));
	}

	public void sendCompileFeedbackMessage(CompileResult compileResult) {
		log.info("sending compileResult feedback, {}", compileResult.isSuccessful());
		template.convertAndSendToUser(compileResult.getUser(), "/queue/compilefeedback",
				new CompileFeedbackMessage(compileResult.getUser(), compileResult.getResult(), compileResult.isSuccessful()));
	}

	public void sendStartToTeams(String taskname) {
		template.convertAndSend("/queue/start", taskname);
	}
	
	public void sendStopToTeams(String taskname) {
		template.convertAndSend("/queue/stop", new TaskMessage(taskname));
	}
	public void sendRefreshToRankingsPage() {
		template.convertAndSend("/queue/rankings", "refresh");
	}
	




	public static class TestFeedbackMessage {
		private String team;
		private String test;
		private String text;
		private boolean success;
		private boolean submit;
		private int score;

		public TestFeedbackMessage() {
		}

		public TestFeedbackMessage(String team, String test, String text, boolean success, Boolean submit, int score) {
			super();
			this.team = team;
			this.test = test;
			this.text = text;
			this.success = success;
			this.submit = submit;
			this.setScore(score);
		}

		public String getTeam() {
			return team;
		}

		public void setTeam(String team) {
			this.team = team;
		}

		public String getTest() {
			return test;
		}

		public void setTest(String test) {
			this.test = test;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public boolean isSubmit() {
			return submit;
		}

		public void setSubmit(boolean submit) {
			this.submit = submit;
		}

		public int getScore() {
			return score;
		}

		public void setScore(int score) {
			this.score = score;
		}
	}

	public static class CompileFeedbackMessage {

		private String team;
		private String text;
		private boolean success;

		public CompileFeedbackMessage(String team, String text, boolean success) {
			super();
			this.team = team;
			this.text = text;
			this.success = success;
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

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}
	}


}