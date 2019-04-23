package nl.moj.server.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The ScoreService calculates the score.
 * <h2>Details</h2>
 * <p>
 * The score service is capable of using penalties and bonuses. They are all calculated against
 * the initial score individually and then the total score is calculated as follows:
 * </p>
 * <pre>  timeLeftInSeconds + totalBonus - totalPenalty</pre>
 * <p>
 * The total score cannot be lower than 0 (zero).
 * </p>
 * <h2>Scoring Rules</h2>
 * <p>The following rules are supported:</p>
 * <ul>
 * <li>submit bonus</li>
 * <li>resubmit penalty</li>
 * <li>test penalty</li>
 * </ul>
 * <h2>Submit Bonus</h2>
 * <p>
 * The submit bonus is applied on a successful (code compiles and all tests pass) submit. Percentage
 * based values ( >= 0%, <= 100%) and numeric values ( >= 0 ) are allowed.
 * </p>
 *
 * <h2>Resubmit Penalty</h2>
 * <p>
 * The submit penalty is applied on submits. Percentage based values ( >= 0%, <= 100%) and numeric
 * values ( >= 0 ) are allowed. The first submit is always free.
 * </p>
 *
 * <h3>Percentage penalty is calculated as follows:</h3>
 * <pre>  timeLeftInSeconds - ((100 - submitPenalty) ^ (submits - 1)) * timeLeftInSeconds</pre>
 *
 * <h3>Fixed penalty is calculated as follows:</h3>
 * <pre>  submitPenalty * (submits - 1)</pre>
 *
 * <h2>TestCase Penalty</h2>
 * <p>The test penalty is applied for every test run being made. It does not matter how many test
 * are run at once.</p>
 *
 * <h3>Percentage penalty is calculated as follows:</h3>
 * <pre>  timeLeftInSeconds - ((100 - submitPenalty) ^ testRuns) * timeLeftInSeconds</pre>
 *
 * <h3>Fixed penalty is calculated as follows:</h3>
 * <pre>  submitPenalty * testRuns</pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {

    private final MojServerProperties mojServerProperties;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final AssignmentResultRepository assignmentResultRepository;

    public void initializeScoreAtStart(Team team, Assignment assignment, CompetitionSession competitionSession) {
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(assignment, competitionSession, team);
        assignmentResultRepository.save(AssignmentResult.builder()
                .assignmentStatus(as)
                .bonus(0L)
                .penalty(0L)
                .initialScore(0L)
                .finalScore(0L)
                .uuid(UUID.randomUUID())
                .build());
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
        AssignmentResult result = assignmentResultRepository.findByTeamAndAssignmentAndCompetitionSession(team, assignment, session);
        result.setInitialScore(score.getInitialScore());
        result.setBonus(score.getSubmitBonus());
        result.setPenalty(score.getTotalPenalty());
        result.setFinalScore(score.getTotalScore());
        assignmentResultRepository.save(result);
        log.info("Registered final score of {} composed of {} for team {} in assignment {}.", score.getTotalScore(), score, team
                .getName(), assignment.getName());
    }

    public Score calculateScore(Team team, ActiveAssignment state, boolean success) {
        if (success) {
            AssignmentDescriptor ad = state.getAssignmentDescriptor();
            AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(), state
                    .getCompetitionSession(), team);
            return Score.builder()
                    .initialScore(state.getTimeRemaining())
                    .submitBonus(calculateSubmitBonus(ad))
                    .resubmitPenalty(calculateSubmitPenalty(ad, state.getTimeRemaining(), as.getSubmitAttempts()
                            .size()))
                    .testPenalty(calculateTestPenalty(ad, state.getTimeRemaining(), as.getTestAttempts().size()))
                    .build();
        }
        return Score.builder().build();
    }

    private Long calculateSubmitPenalty(AssignmentDescriptor ad, Long initialScore, Integer submits) {
        if (submits != null && submits > 1 && ad.getScoringRules().getResubmitPenalty() != null) {
            String penalty = ad.getScoringRules().getResubmitPenalty().trim();
            try {
                // the first submit is always free, hence submits - 1.
                return calculatePenaltyValue(initialScore, submits - 1, penalty);
            } catch (Exception nfe) {
                log.warn("Cannot use submit penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty);
                log.trace("Cannot use submit penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty, nfe);
            }
        }
        return 0L;
    }

    private Long calculateTestPenalty(AssignmentDescriptor ad, Long initialScore, Integer testRuns) {
        if (testRuns != null && testRuns > 0 && ad.getScoringRules().getTestPenalty() != null) {
            String penalty = ad.getScoringRules().getTestPenalty().trim();
            try {
                return calculatePenaltyValue(initialScore, testRuns, penalty);
            } catch (Exception nfe) {
                log.warn("Cannot use test penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty);
                log.trace("Cannot use test penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty, nfe);
            }
        }
        return 0L;
    }

    private Long calculatePenaltyValue(Long initialScore, Integer count, String penalty) throws NumberFormatException {
        if (penalty.endsWith("%") && initialScore != null && initialScore > 0 && count > 0) {
            Long p = 100L - Long.valueOf(penalty.substring(0, penalty.length() - 1));
            if (p < 0) {
                throw new IllegalArgumentException("Penalty percentage value must be <= 100%");
            }
            return initialScore - Math.round(initialScore * Math.pow((p.doubleValue() / 100.0), count.doubleValue()));
        } else {
            Long p = Long.valueOf(penalty);
            if (p < 0) {
                throw new IllegalArgumentException("Penalty value must be >= 0.");
            }
            return p * count;
        }
    }

    private long calculateSubmitBonus(AssignmentDescriptor ad) {
        long submitBonus;
        if (ad.getScoringRules().getSuccessBonus() != null) {
            submitBonus = ad.getScoringRules().getSuccessBonus();
        } else {
            submitBonus = mojServerProperties.getCompetition().getSuccessBonus();
        }
        return submitBonus;
    }
}
