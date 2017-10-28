package nl.moj.server.competition;

import nl.moj.server.persistence.ResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {

	private static final Logger log = LoggerFactory.getLogger(ScoreService.class);
	private final Competition competition;
	private final ResultMapper resultMapper;

    @Value("${moj.server.competition.bonusForSuccessfulSubmission}")
	private int bonusForSuccessfulSubmission;

	public ScoreService(Competition competition, ResultMapper resultMapper) {
		this.competition = competition;
		this.resultMapper = resultMapper;
	}

	public Integer registerScoreAtSubmission(String teamname, int scoreAtSubmissionTime) {
		String assignment = competition.getCurrentAssignment().getName();
		Integer oldScore = resultMapper.getScore(teamname, assignment);
		if (oldScore == null) {
            oldScore = 0;
        }
		final int assignmentScore = scoreAtSubmissionTime + bonusForSuccessfulSubmission;
		final int newScore = oldScore + assignmentScore;
		log.info("Team {} submitted {}. Previous score {} + assignment score {} + bonus {} = {}",
		        teamname, assignment, oldScore, scoreAtSubmissionTime, bonusForSuccessfulSubmission, newScore );
        resultMapper.updateScore(teamname, assignment, newScore);
        //resultMapper.insertScore(teamname, assignment, assignmentScore);
        return assignmentScore;
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
