package nl.moj.server.rankings;

import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import nl.moj.server.competition.Competition;
import nl.moj.server.model.Team;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TeamMapper;
import nl.moj.server.test.TestResult;

@Controller
public class RankingsController {
	private final static Logger log = LoggerFactory.getLogger(RankingsController.class);
	
	@Autowired
	private ResultMapper resultMapper;
	
	@Autowired
	private TeamMapper teamMapper;
	
	@Autowired
	private SimpMessagingTemplate template;
	
	@Autowired
	private Competition competition;
	
	@GetMapping("/rankings")
	public String getRankings(Model model){
		List<Team> teams = teamMapper.getAllTeams();//new ArrayList<>();
//		for(String team : teamMapper.getAllUserNames()){
//			teams.add(teamMapper.getTeam(team));
//		}
		model.addAttribute("assignments", competition.getAssignmentNames());
		model.addAttribute("teamMapper", teamMapper);
		model.addAttribute("teams", teams);
		return "rankings";
	}
	
	public void updateScoreBoard(TestResult testResult){
		String teamname = testResult.getUser();
		String assignment = competition.getCurrentAssignment().getName();
		Integer score = resultMapper.getScore(teamname, assignment);
		if(score == null)
			score = 0;
		resultMapper.updateScore(teamname, assignment, score + 100);
		refreshScoreBoard();
	}
	
	public void refreshScoreBoard() {
		log.info("refreshScoreBoard ");
		template.convertAndSend("/rankings", "refresh");
	}
}
