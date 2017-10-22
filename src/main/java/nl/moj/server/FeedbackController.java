package nl.moj.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import nl.moj.server.competition.Competition;
import nl.moj.server.persistence.TeamMapper;

@Controller
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
		model.addObject("tests", competition.getCurrentAssignment().getTestNames());

		return model;
	}

	public void sendTestFeedback(String team, String test, Boolean success) {
		TestFeedbackMessage testFeedback = new TestFeedbackMessage(team, test, success);
		template.convertAndSend("/queue/testfeedback", testFeedback);
	}

	public static class TestFeedbackMessage {
		private String team;
		private String test;
		private Boolean success;

		public TestFeedbackMessage() {
		}

		public TestFeedbackMessage(String team, String test, Boolean success) {
			super();
			this.team = team;
			this.test = test;
			this.success = success;
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

		public Boolean isSuccess() {
			return success;
		}

		public void setSuccess(Boolean success) {
			this.success = success;
		}

	}
}
