package nl.moj.server.runtime;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.runtime.model.Result;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreService {

	private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

	private final ResultRepository resultRepository;

	private final TeamRepository teamRepository;

	private final MojServerProperties mojServerProperties;

	public void removeScoresForAssignment(Assignment assignment) {
		resultRepository.findAllByAssignment(assignment).forEach(resultRepository::delete);
	}

	public void initializeScoreAtStart(Team team, Assignment assignment, CompetitionSession competitionSession) {
		Result result = new Result();
		result.setAssignment(assignment);
		result.setCompetitionSession(competitionSession);
		result.setTeam(team);
		result.setScore(0);
		result.setPenalty(0);
		result.setCredit(0);
        resultRepository.save(result);
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
	public Long registerScoreAtSubmission(String teamname, Assignment assignment, Long scoreAtSubmissionTime) {
		Long score = 0L;
		if (scoreAtSubmissionTime > 0) {
			score = scoreAtSubmissionTime + mojServerProperties.getCompetition().getSuccessBonus();
			
		}
		log.debug("Team {} submitted {}. assignment score {} + bonus {} = {}",
		        teamname, assignment, scoreAtSubmissionTime, mojServerProperties.getCompetition().getSuccessBonus(), score );
        Result result = resultRepository.findByTeamAndAssignment(teamRepository.findByName(teamname), assignment);
        // TODO fix possible precision loss.
        result.setScore(score.intValue());
        resultRepository.save(result);
        return score;
	}
}
