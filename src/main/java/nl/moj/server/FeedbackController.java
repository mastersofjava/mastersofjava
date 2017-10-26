package nl.moj.server;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.persistence.TeamMapper;
import nl.moj.server.test.TestResult;

@Controller
@Slf4j
public class FeedbackController {

	@Autowired
	private TeamMapper teamMapper;

	@Autowired
	private Competition competition;

	@Autowired
	private SimpMessagingTemplate template;

	@GetMapping("/feedback")
	public ModelAndView feedback() {
		ModelAndView model = new ModelAndView("testfeedback");
		model.addObject("teams", teamMapper.getAllTeams());
		List<String> testNames = new ArrayList<>();
		if (competition.getCurrentAssignment() != null) {
			testNames = competition.getCurrentAssignment().getTestNames();
		}
		model.addObject("tests", testNames);

		return model;
	}

	public void sendTestFeedbackMessage(TestResult testResult, Boolean submit) {
		log.info("sending testResult feedback");
		template.convertAndSendToUser(testResult.getUser(), "/queue/feedback", new TestFeedbackMessage(
				testResult.getUser(), testResult.getTestname(), testResult.getTestResult(), testResult.isSuccessful(), submit));
		template.convertAndSend("/queue/testfeedback", new TestFeedbackMessage(testResult.getUser(),
				testResult.getTestname(), null, testResult.isSuccessful(), submit));
	}

	public void sendCompileFeedbackMessage(CompileResult compileResult) {
		log.info("sending compileResult feedback, {}", compileResult.isSuccessful());
		template.convertAndSendToUser(compileResult.getUser(), "/queue/compilefeedback",
				new CompileFeedbackMessage(compileResult.getUser(), compileResult.getCompileResult(), compileResult.isSuccessful()));
	}

	public static class TestFeedbackMessage {
		private String team;
		private String test;
		private String text;
		private Boolean success;
		private Boolean submit;

		public TestFeedbackMessage() {
		}

		public TestFeedbackMessage(String team, String test, String text, Boolean success, Boolean submit) {
			super();
			this.team = team;
			this.test = test;
			this.text = text;
			this.success = success;
			this.submit = submit;
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

		public Boolean isSuccess() {
			return success;
		}

		public void setSuccess(Boolean success) {
			this.success = success;
		}

		public Boolean getSubmit() {
			return submit;
		}

		public void setSubmit(Boolean submit) {
			this.submit = submit;
		}

	}

	public class CompileFeedbackMessage {

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
