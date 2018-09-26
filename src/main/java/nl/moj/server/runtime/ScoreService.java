package nl.moj.server.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.Result;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.teams.model.Team;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {

	private final ResultRepository resultRepository;

	private final MojServerProperties mojServerProperties;

	public void removeScoresForAssignment(Assignment assignment, CompetitionSession competitionSession) {
		resultRepository.findAllByAssignmentAndCompetitionSession(assignment,competitionSession).forEach(resultRepository::delete);
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
     *
     * @param team
	 * @param assignment
	 * @param session
	 * @param score
     */
    public void registerScore(Team team, Assignment assignment, CompetitionSession session, Score score) {
		Result result = resultRepository.findByTeamAndAssignmentAndCompetitionSession(team, assignment, session);
		result.setScore(score.getFinalScore().intValue());
		log.debug("Registered final score of {} for team {} in assignment {}.", score.getFinalScore(), team.getName(), assignment.getName());
	}

	public Score calculateScore(Team team, AssignmentState state, CompetitionSession session, boolean success) {
		if( success ) {
			AssignmentDescriptor ad = state.getAssignmentDescriptor();
			long submitBonus;
			if (ad.getScoringRules().getSuccessBonus() != null && ad.getScoringRules().getSuccessBonus() > 0) {
				submitBonus = ad.getScoringRules().getSuccessBonus();
			} else {
				submitBonus = mojServerProperties.getCompetition().getSuccessBonus();
			}

			return Score.builder()
					.timeRemaining(state.getTimeRemaining())
					.submitBonus(submitBonus)
					.build();
		}
		return Score.builder().build();
	}
}
