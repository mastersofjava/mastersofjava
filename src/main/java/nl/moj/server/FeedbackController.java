package nl.moj.server;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import nl.moj.server.competition.Competition;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.model.Team;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TeamMapper;
import nl.moj.server.test.TestResult;

@Controller
public class FeedbackController {

	private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);

	@Autowired
	private TeamMapper teamMapper;
	
	@Autowired
	private ResultMapper resultMapper;

	@Autowired
	private Competition competition;

	@Autowired
	private SimpMessagingTemplate template;

	@GetMapping("/feedback")
	public ModelAndView feedback() {
		ModelAndView model = new ModelAndView("testfeedback");
		List<Team> allTeams = teamMapper.getAllTeams();
		orderTeamsByHighestTotalScore(allTeams);
		model.addObject("teams", allTeams);
		List<String> testNames = new ArrayList<>();
		if (competition.getCurrentAssignment() != null) {
			testNames = competition.getCurrentAssignment().getTestNames();
		}
		model.addObject("tests", testNames);

		return model;
	}

	public void sendTestFeedbackMessage(TestResult testResult, Boolean submit, Integer score) {
		log.info("sending testResult feedback");
		//Integer totalScore = resultMapper.getScoreForAssignment(testResult.getUser(), competition.getCurrentAssignment().getName());
		
		template.convertAndSendToUser(testResult.getUser(), "/queue/feedback", new TestFeedbackMessage(
				testResult.getUser(), testResult.getTestname(), testResult.getTestResult(), testResult.isSuccessful(), submit, score));
		template.convertAndSend("/queue/testfeedback", new TestFeedbackMessage(testResult.getUser(),
				testResult.getTestname(), null, testResult.isSuccessful(), submit, score));
	}

	public void sendCompileFeedbackMessage(CompileResult compileResult) {
		log.info("sending compileResult feedback, {}", compileResult.isSuccessful());
		template.convertAndSendToUser(compileResult.getUser(), "/queue/compilefeedback",
				new CompileFeedbackMessage(compileResult.getUser(), compileResult.getCompileResult(), compileResult.isSuccessful()));
	}

	private void orderTeamsByHighestTotalScore(List<Team> allTeams) {
		allTeams.stream()
			.sorted( (t1, t2) -> Integer.compare(
					resultMapper.getTotalScore(t1.getName()), resultMapper.getTotalScore(t2.getName())
				)
			);
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

	private class CompileFeedbackMessage {

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
