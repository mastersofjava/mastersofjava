package nl.moj.server.competition;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.persistence.ResultMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {

	private final Competition competition;

	private final ResultMapper resultMapper;

    @Value("${moj.server.competition.bonusForSuccessfulSubmission}")
	private int bonusForSuccessfulSubmission;

	public void registerScoreAtSubmission(String teamname, int scoreAtSubmissionTime) {
		String assignment = competition.getCurrentAssignment().getName();
		Integer oldScore = resultMapper.getScore(teamname, assignment);
		if (oldScore == null) {
            oldScore = 0;
        }
		final int newScore = oldScore + scoreAtSubmissionTime + bonusForSuccessfulSubmission;
		log.info("Team {} submitted {}. Previous score {} + assignment score {} + bonus {} = {}",
		        teamname, assignment, oldScore, scoreAtSubmissionTime, bonusForSuccessfulSubmission, newScore );
        resultMapper.updateScore(teamname, assignment, newScore);
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
