package nl.moj.server.competition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.persistence.ResultMapper;

@Service
public class ScoreService {

	@Autowired
	private Competition competition;
	
	@Autowired
	private ResultMapper resultMapper;
	
	public void subtractSpentSeconds(String teamname) {
		int newscore = competition.getRemainingTime();
		String assignment = competition.getCurrentAssignment().getName();
		Integer score = resultMapper.getScore(teamname, assignment);
		if (score == null)
			score = 0;
		resultMapper.updateScore(teamname, assignment, score + newscore);

	}

	public void applyTestPenaltyOrCredit(String teamname) {
		if (competition.getCurrentAssignment().hasTestPenalties()) {
			resultMapper.incrementPenalty(teamname, competition.getCurrentAssignment().getName(), 1);
		}
		if (competition.getCurrentAssignment().hasTestCredits()) {
			resultMapper.decrementCredit(teamname, competition.getCurrentAssignment().getName(), 1);
		}
		
	}
	
	
}
