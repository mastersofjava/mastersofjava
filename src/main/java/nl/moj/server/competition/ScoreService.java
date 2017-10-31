package nl.moj.server.competition;

import nl.moj.server.persistence.ResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {

	private static final Logger log = LoggerFactory.getLogger(ScoreService.class);
	private final ResultMapper resultMapper;

    @Value("${moj.server.competition.bonusForSuccessfulSubmission}")
	private int bonusForSuccessfulSubmission;

	public ScoreService(ResultMapper resultMapper) {
		this.resultMapper = resultMapper;
	}

	public void removeScoresForAssignment(String assignment) {
		resultMapper.deleteResultsByAssignment(assignment);
	}

	public void initializeScoreAtStart(String teamname, String assignment) {
		resultMapper.insertScore(teamname,assignment, 0);
	}

	/**
	 * Scores can only be registered once per assignment per team.
	 * Old score is always 0
	 * new score is seconds left plus bonus.
	 * 
	 * @param teamname
	 * @param assignment
	 * @param scoreAtSubmissionTime
	 * @return
	 */
	public Integer registerScoreAtSubmission(String teamname, String assignment, int scoreAtSubmissionTime) {
		int score = 0;
		if (scoreAtSubmissionTime > 0) {
			score = scoreAtSubmissionTime + bonusForSuccessfulSubmission;
			
		}
		log.debug("Team {} submitted {}. assignment score {} + bonus {} = {}",
		        teamname, assignment, scoreAtSubmissionTime, bonusForSuccessfulSubmission, score );
        resultMapper.updateScore(teamname, assignment, score);
        return score;
	}
}
