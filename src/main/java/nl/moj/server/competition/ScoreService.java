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

	public Integer registerScoreAtSubmission(String teamname, String assignment, int scoreAtSubmissionTime) {
		Integer oldScore = resultMapper.getScore(teamname, assignment);
		if (oldScore == null) {
            oldScore = 0;
        }
		final int assignmentScore = scoreAtSubmissionTime + bonusForSuccessfulSubmission;
		final int newScore = oldScore + assignmentScore;
		log.info("Team {} submitted {}. Previous score {} + assignment score {} + bonus {} = {}",
		        teamname, assignment, oldScore, scoreAtSubmissionTime, bonusForSuccessfulSubmission, newScore );
        resultMapper.updateScore(teamname, assignment, assignmentScore);
        return assignmentScore;
	}
}
