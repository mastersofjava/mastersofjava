package nl.moj.server.rankings;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import nl.moj.server.competition.Competition;
import nl.moj.server.model.Ranking;
import nl.moj.server.persistence.RankingMapper;

@Controller
public class RankingsController {
	private final static Logger log = LoggerFactory.getLogger(RankingsController.class);
	
	@Autowired
	private RankingMapper rankingMapper;
	
	
	@Autowired
	private Competition competition;
	
	
	@GetMapping("/rankings")
	public String getRankings(Model model){
		List<Ranking> rankings = rankingMapper.getRankings();
		model.addAttribute("assignments", competition.getAssignmentNames());
		model.addAttribute("rankings", rankings);
		return "rankings";
	}
	

}
