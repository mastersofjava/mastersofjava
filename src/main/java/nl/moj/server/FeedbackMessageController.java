package nl.moj.server;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.TaskControlController.TaskMessage;
import nl.moj.server.compiler.CompileResult;
import nl.moj.server.message.CompileFeedbackMessage;
import nl.moj.server.message.TaskTimeMessage;
import nl.moj.server.message.TestFeedbackMessage;
import nl.moj.server.test.TestResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class FeedbackMessageController {


	private SimpMessagingTemplate template;

	public FeedbackMessageController(SimpMessagingTemplate template) {
		super();
		this.template = template;
	}

	public void sendTestFeedbackMessage(TestResult testResult, Boolean submit, Integer score) {
		log.info("sending testResult feedback");
		template.convertAndSendToUser(testResult.getUser(), "/queue/feedback",
				new TestFeedbackMessage(testResult.getUser(), testResult.getTestname(), testResult.getResult(),
						testResult.isSuccessful(), submit, score, testResult.getRemainingResubmits()));
		template.convertAndSend("/queue/feedbackpage", new TestFeedbackMessage(testResult.getUser(),
				testResult.getTestname(), null, testResult.isSuccessful(), submit, score, testResult.getRemainingResubmits()));
	}

	public void sendCompileFeedbackMessage(CompileResult compileResult) {
		log.info("sending compileResult feedback, {}", compileResult.isSuccessful());
		template.convertAndSendToUser(compileResult.getUser(), "/queue/compilefeedback",
				new CompileFeedbackMessage(compileResult.getUser(), compileResult.getResult(),
						compileResult.isSuccessful(), !compileResult.getTests().isEmpty()));
	}

	public void sendDisableFeedbackMessage(TestResult testResult) {
		log.info("sending disable submit feedback to (), assignment successful? {}", testResult.getUser(), testResult.isSuccessful());
		template.convertAndSendToUser(testResult.getUser(), "/queue/disable",
				new CompileFeedbackMessage(testResult.getUser(), testResult.getResult(),
						testResult.isSuccessful(), false));
	}

	public void sendStartToTeams(String taskname) {
		template.convertAndSend("/queue/start", taskname);
	}

	public void sendStopToTeams(String taskname) {
		template.convertAndSend("/queue/stop", new TaskMessage(taskname));
	}

	public void sendRefreshToRankingsPage() {
		log.info("sending refresh to /queue/rankings");
		template.convertAndSend("/queue/rankings", "refresh");
	}

	public void sendRemainingTime(Long remainingTime, Long totalTime) {
		try {
			log.info("Sending remaining time: r={}, t={}", remainingTime, totalTime);
			TaskTimeMessage taskTimeMessage = new TaskTimeMessage();
			taskTimeMessage.setRemainingTime(String.valueOf(remainingTime));
			taskTimeMessage.setTotalTime(String.valueOf(totalTime));
			template.convertAndSend("/queue/time", taskTimeMessage);
		} catch( Exception e ) {
			log.warn("Failed to send remaining time.", e);
		}
	}

}