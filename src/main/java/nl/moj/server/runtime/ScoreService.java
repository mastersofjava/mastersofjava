package nl.moj.server.runtime;

import lombok.RequiredArgsConstructor;
import nl.moj.server.model.Result;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.teams.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreService {

	private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

	private final ResultRepository resultRepository;

	private final TeamRepository teamRepository;

    @Value("${moj.server.competition.bonusForSuccessfulSubmission}")
	private int bonusForSuccessfulSubmission;

	public void removeScoresForAssignment(String assignment) {
		resultRepository.findAllByAssignment(assignment).forEach(resultRepository::delete);
	}

	public void initializeScoreAtStart(String teamname, String assignment) {
        resultRepository.save(new Result(teamRepository.findByName(teamname), assignment, 0, 0, 0));
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
	public Long registerScoreAtSubmission(String teamname, String assignment, Long scoreAtSubmissionTime) {
		Long score = 0L;
		if (scoreAtSubmissionTime > 0) {
			score = scoreAtSubmissionTime + bonusForSuccessfulSubmission;
			
		}
		log.debug("Team {} submitted {}. assignment score {} + bonus {} = {}",
		        teamname, assignment, scoreAtSubmissionTime, bonusForSuccessfulSubmission, score );
        Result result = resultRepository.findByTeamAndAssignment(teamRepository.findByName(teamname), assignment);
        // TODO fix possible precision loss.
        result.setScore(score.intValue());
        resultRepository.save(result);
        return score;
	}
}
