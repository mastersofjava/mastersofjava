package nl.moj.server;

import lombok.RequiredArgsConstructor;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.CollectionUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static nl.moj.server.model.Role.ROLE_USER;

@Controller
@RequiredArgsConstructor
public class FeedbackController {

    private final TeamRepository teamRepository;

    private final ResultRepository resultRepository;

	private final CompetitionRuntime competition;

	@GetMapping("/feedback")
	public ModelAndView feedback() {
		ModelAndView model = new ModelAndView("testfeedback");
		List<Team> allTeams = teamRepository.findAllByRole(ROLE_USER);
		orderTeamsByHighestTotalScore(allTeams);

		List<List<Team>> partitionedTeams = CollectionUtil.partition(allTeams, 3);
		model.addObject("teams1", partitionedTeams.get(0));
        model.addObject("teams2", partitionedTeams.get(1));
        model.addObject("teams3", partitionedTeams.get(2));

        List<String> testNames = new ArrayList<>();

        if (competition.getCurrentAssignment() != null) {
        	AssignmentState state = competition.getAssignmentState();

            testNames = state.getTestNames();

            model.addObject("assignment", state.getAssignmentDescriptor().getName());
            model.addObject("timeLeft", state.getTimeRemaining());
            model.addObject("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addObject("running", state.isRunning());
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
		allTeams.sort(Comparator.comparingInt(t -> resultRepository.getTotalScore(t)));
	}
	

}
