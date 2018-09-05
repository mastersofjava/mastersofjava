package nl.moj.server.rankings;

import lombok.RequiredArgsConstructor;
import nl.moj.server.model.Ranking;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.util.CollectionUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class RankingsController {

	private final CompetitionRuntime competition;

	private final RankingsMapper rankingsMapper;

	@GetMapping("/rankings")
	public String getRankings(Model model){
		List<Ranking> rankings = enrich(rankingsMapper.getRankings());

		model.addAttribute("assignments", competition.getAssignmentNames());
		model.addAttribute("top", rankings.subList(0,Math.min(5, rankings.size())));

		List<List<Ranking>> parts = partitionRemaining(rankings, 5);
		model.addAttribute("bottom1", parts.get(0));
		model.addAttribute("bottom2", parts.get(1));
		model.addAttribute("bottom3", parts.get(2));
		model.addAttribute("bottom4", parts.get(3));
		if( competition.getCurrentAssignment() != null ) {
			AssignmentState state = competition.getAssignmentState();
			model.addAttribute("assignment", state.getAssignmentDescriptor().getName());
			model.addAttribute("timeLeft", state.getTimeRemaining());
			model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
			model.addAttribute("running", state.isRunning());
		} else {
			model.addAttribute("assignment", "-");
			model.addAttribute("timeLeft", 0);
			model.addAttribute("time", 0);
			model.addAttribute("running", false);
		}
		return "rankings";
	}

	private List<List<Ranking>> partitionRemaining(List<Ranking> rankings, int offset) {
		List<Ranking> remaining = new ArrayList<>();
		if( rankings.size() > offset ) {
			remaining = rankings.subList(offset,rankings.size());
		}
		return CollectionUtil.partition(remaining,4);
	}

	private List<Ranking> enrich( List<Ranking> rankings ) {
		for( int i=0; i<rankings.size(); i++) {
			rankings.get(i).setRank(i+1);
		}
		return rankings;
	}
}
