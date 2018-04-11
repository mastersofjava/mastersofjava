package nl.moj.server.rankings;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.Competition;
import nl.moj.server.model.Ranking;
import nl.moj.server.util.CollectionUtil;

@Controller
@RequiredArgsConstructor
public class RankingsController {

	private final Competition competition;

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
			model.addAttribute("assignment", competition.getCurrentAssignment().getName());
			model.addAttribute("timeLeft", competition.getRemainingTime());
			model.addAttribute("time", competition.getCurrentAssignment().getSolutionTime());
			model.addAttribute("running", competition.getCurrentAssignment().isRunning());
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
