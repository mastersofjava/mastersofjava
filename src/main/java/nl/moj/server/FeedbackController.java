package nl.moj.server;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import nl.moj.server.competition.Competition;
import nl.moj.server.model.Team;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TeamMapper;
import nl.moj.server.util.CollectionUtil;

@Controller
public class FeedbackController {

	private TeamMapper teamMapper;
	
	private ResultMapper resultMapper;

	private Competition competition;

	public FeedbackController(TeamMapper teamMapper, ResultMapper resultMapper, Competition competition) {
		super();
		this.teamMapper = teamMapper;
		this.resultMapper = resultMapper;
		this.competition = competition;
	}

	@GetMapping("/feedback")
	public ModelAndView feedback() {
		ModelAndView model = new ModelAndView("testfeedback");
		List<Team> allTeams = teamMapper.getAllTeams();
		orderTeamsByHighestTotalScore(allTeams);

		List<List<Team>> partitionedTeams = CollectionUtil.partition(allTeams, 3);
		model.addObject("teams1", partitionedTeams.get(0));
        model.addObject("teams2", partitionedTeams.get(1));
        model.addObject("teams3", partitionedTeams.get(2));

        List<String> testNames = new ArrayList<>();

        if (competition.getCurrentAssignment() != null) {
            testNames = competition.getCurrentAssignment().getTestNames();
            model.addObject("assignment", competition.getCurrentAssignment().getName());
            model.addObject("timeLeft", competition.getRemainingTime());
            model.addObject("time", competition.getCurrentAssignment().getSolutionTime());
            model.addObject("running", competition.getCurrentAssignment().isRunning());
        } else {
            model.addObject("assignment", "-");
            model.addObject("timeLeft", 0);
            model.addObject("time", 0);
            model.addObject("running", false);
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
