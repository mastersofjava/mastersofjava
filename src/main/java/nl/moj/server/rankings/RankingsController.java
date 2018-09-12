package nl.moj.server.rankings;

import lombok.RequiredArgsConstructor;
import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.rankings.service.RankingsService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.CompetitionState;
import nl.moj.server.util.CollectionUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class RankingsController {

	private final CompetitionRuntime competition;

	private final RankingsService rankingsMapper;

	@GetMapping("/rankings")
	public ModelAndView getRankings() {
		List<Ranking> rankings = enrich(rankingsMapper.getRankings(competition.getCompetitionSession()));
		CompetitionState competitionState = competition.getCompetitionState();
		ModelAndView model = new ModelAndView("rankings");
		if( competitionState.getCompletedAssignments().isEmpty() ) {
			model.addObject("oas", Collections.emptyList());
		} else {
			model.addObject("oas", competitionState.getCompletedAssignments());
		}
		model.addObject("top", rankings.subList(0, Math.min(5, rankings.size())));

		List<List<Ranking>> parts = partitionRemaining(rankings, 5);
		model.addObject("bottom1", parts.get(0));
		model.addObject("bottom2", parts.get(1));
		model.addObject("bottom3", parts.get(2));
		model.addObject("bottom4", parts.get(3));
		if (competition.getCurrentAssignment() != null) {
			AssignmentState state = competition.getAssignmentState();
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
		return model;
	}

	private List<List<Ranking>> partitionRemaining(List<Ranking> rankings, int offset) {
		List<Ranking> remaining = new ArrayList<>();
		if (rankings.size() > offset) {
			remaining = rankings.subList(offset, rankings.size());
		}
		return CollectionUtil.partition(remaining, 4);
	}

	private List<Ranking> enrich(List<Ranking> rankings) {
		for (int i = 0; i < rankings.size(); i++) {
			rankings.get(i).setRank(i + 1);
		}
		return rankings;
	}
}
