package nl.moj.server.runtime;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.runtime.model.AssignmentState;
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
     * Old score is always 0
     * new score is seconds left plus bonus.
     *
     * @param team
     * @param state
     * @return a Long, the final score.
     */
    public Long registerScoreAtSubmission(Team team, AssignmentState state, CompetitionSession session) {
        Long score = 0L;
        Integer bonusForSuccessfulSubmission = 0;
        Long submissionTime = state.getTimeRemaining();
        AssignmentDescriptor ad = state.getAssignmentDescriptor();
        Assignment assignment = state.getAssignment();
        if (submissionTime > 0) {
            score = submissionTime;
			if( ad.getScoringRules().getSuccessBonus() != null && ad.getScoringRules().getSuccessBonus() > 0) {
				bonusForSuccessfulSubmission = ad.getScoringRules().getSuccessBonus();
			} else {
				bonusForSuccessfulSubmission = ad.getScoringRules().getSuccessBonus();
			}
			score += bonusForSuccessfulSubmission;
        }
        Result result = resultRepository.findByTeamAndAssignmentAndCompetitionSession(team, assignment, session);
        // TODO fix possible precision loss.
        result.setScore(score.intValue());
        resultRepository.save(result);
        log.debug("Saved score for Team {} in assignment {}. Score {} + bonus {} = {}",
                team.getName(), assignment.getName(), submissionTime, bonusForSuccessfulSubmission, score );
        return score;
    }

//    public Long calculateScore(Team team, Assignment assignment, Long submissionTime) {
//        Long score = 0L;
//        if (submissionTime > 0) {
//            score = submissionTime + bonusForSuccessfulSubmission;
//        }
//        log.debug("Team {} submitted {} and scored {} + bonus {} = {}", team.getName(), assignment.getName(), submissionTime, bonusForSuccessfulSubmission, score);
//        return score;
//    }

//    public Long setFinalAssignmentScore(TestResult testResult, Assignment assignment, AssignmentDescriptor ad, Long scoreAtSubmissionTime) {
//		Long score = registerScoreAtSubmission(teamRepository.findByName(testResult.getUser()), assignment, ad, testResult.isSuccessful() ? scoreAtSubmissionTime : 0L);
//		return score;
//	}

	public Long calculateScore(Team team, AssignmentState state) {
		return 0L;
	}
}
