package nl.moj.server;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import nl.moj.server.competition.Competition;
import nl.moj.server.model.Team;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TeamMapper;

@Controller
public class FeedbackController {

	@Autowired
	private TeamMapper teamMapper;
	
	@Autowired
	private ResultMapper resultMapper;

	@Autowired
	private Competition competition;


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

	private void orderTeamsByHighestTotalScore(List<Team> allTeams) {
		allTeams.stream()
			.sorted( (t1, t2) -> Integer.compare(
					resultMapper.getTotalScore(t1.getName()), resultMapper.getTotalScore(t2.getName())
				)
			);
	}
	

}
