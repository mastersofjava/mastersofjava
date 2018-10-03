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
		result.setScore(score.getTotalScore().intValue());
		result.setCredit(score.getInitialScore().intValue());
		result.setPenalty(score.getTotalPenalty().intValue());
		resultRepository.save(result);
		log.debug("Registered final score of {} for team {} in assignment {}.", score.getTotalScore(), team.getName(), assignment.getName());
	}

	public Score calculateScore(Team team, AssignmentState state, boolean success) {
		if( success ) {
			AssignmentDescriptor ad = state.getAssignmentDescriptor();
			return Score.builder()
					.initialScore(state.getTimeRemaining())
					.submitBonus(calculateSubmitBonus(ad))
					.resubmitPenalty(calculateSubmitPenalty(ad,state.getTimeRemaining(),state.getTeamStatus(team).getSubmits()))
					.build();
		}
		return Score.builder().build();
	}

	private Long calculateSubmitPenalty(AssignmentDescriptor ad, Long initialScore, Integer submits) {
		if( submits != null && submits > 1 && ad.getScoringRules().getResubmitPenalty() != null ) {
			Integer resubmits = submits -1;
			String penalty = ad.getScoringRules().getResubmitPenalty().trim();
			try {
				if (penalty.endsWith("%") && initialScore != null && initialScore > 0) {
					Long p = Long.valueOf(penalty.substring(0, penalty.length() - 1));
					return initialScore - Math.round(initialScore * Math.pow((p.doubleValue() / 100.0), resubmits.doubleValue()));
				} else {
					Long p = Long.valueOf(penalty);
					return p * (submits - 1);
				}
			} catch( NumberFormatException nfe ) {
				log.warn("Cannot use submit penalty from '"+penalty+"'. Expected a number or percentage, ignoring and using a value of 0.", nfe);
			}
		}
    	return 0L;
	}

	private long calculateSubmitBonus(AssignmentDescriptor ad) {
		long submitBonus;
		if (ad.getScoringRules().getSuccessBonus() != null ) {
			submitBonus = ad.getScoringRules().getSuccessBonus();
		} else {
			submitBonus = mojServerProperties.getCompetition().getSuccessBonus();
		}
		return submitBonus;
	}
}
