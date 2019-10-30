package nl.moj.server.runtime;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.test.model.TestCase;
import org.springframework.stereotype.Service;

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
 *
 * <h3>TestCase Bonus</h3>
 * <p>The test case bonus is given for success test cases. The testcase results are taken from the
 * most recent submit attempt.</p>
 * <pre>  max(round(timeLeftInSeconds * 5%),10) * nrSuccessTests </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {

    private final MojServerProperties mojServerProperties;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final AssignmentResultRepository assignmentResultRepository;

    public void initializeScoreAtStart(AssignmentStatus as) {
        AssignmentResult ar = assignmentResultRepository.save(AssignmentResult.builder()
                .assignmentStatus(as)
                .bonus(0L)
                .penalty(0L)
                .initialScore(0L)
                .finalScore(0L)
                .uuid(UUID.randomUUID())
                .build());
        as.setAssignmentResult(ar);
    }

    private Score calculateScore(ActiveAssignment state, AssignmentStatus as) {
        AssignmentDescriptor ad = state.getAssignmentDescriptor();
        return Score.builder()
                .initialScore(calculateInitialScore(state.getTimeRemaining(), as))
                .submitBonus(calculateSubmitBonus(ad, as))
                .testBonus(calculateTestBonus(state.getTimeRemaining(), as))
                .resubmitPenalty(calculateSubmitPenalty(ad, state.getTimeRemaining(), as))
                .testPenalty(calculateTestPenalty(ad, state.getTimeRemaining(), as))
                .build();
    }

    private Long calculateInitialScore(Long timeRemaining, AssignmentStatus as) {
        if (isSuccessSubmit(as)) {
            return timeRemaining;
        }
        return 0L;
    }

    private Long calculateSubmitPenalty(AssignmentDescriptor ad, Long initialScore, AssignmentStatus as) {
        if (isSuccessSubmit(as)) {
            int submits = as.getSubmitAttempts().size();
            if (submits > 1 && ad.getScoringRules().getResubmitPenalty() != null) {
                String penalty = ad.getScoringRules().getResubmitPenalty().trim();
                try {
                    // the first submit is always free, hence submits - 1.
                    return calculatePenaltyValue(initialScore, submits - 1, penalty);
                } catch (Exception nfe) {
                    log.warn("Cannot use submit penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty);
                    log.trace("Cannot use submit penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty, nfe);
                }
            }
        }
        return 0L;
    }

    private Long calculateTestPenalty(AssignmentDescriptor ad, Long initialScore, AssignmentStatus as) {
        if (isSuccessSubmit(as)) {
            // get the test run count, the submit test is free, hence test runs - submit attempts
            int testRuns = as.getTestAttempts().size() - as.getSubmitAttempts().size();
            if (testRuns > 0 && ad.getScoringRules().getTestPenalty() != null) {
                String penalty = ad.getScoringRules().getTestPenalty().trim();
                try {
                    return calculatePenaltyValue(initialScore, testRuns, penalty);
                } catch (Exception nfe) {
                    log.warn("Cannot use test penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty);
                    log.trace("Cannot use test penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty, nfe);
                }
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

    private long calculateSubmitBonus(AssignmentDescriptor ad, AssignmentStatus as) {
        if (isSuccessSubmit(as)) {
            long submitBonus;
            if (ad.getScoringRules().getSuccessBonus() != null) {
                submitBonus = ad.getScoringRules().getSuccessBonus();
            } else {
                submitBonus = mojServerProperties.getCompetition().getSuccessBonus();
            }
            return submitBonus;
        }
        return 0L;
    }

    // Test bonus is given even if submit attempt failed.
    private long calculateTestBonus(Long initialScore, AssignmentStatus as) {
        Optional<SubmitAttempt> sa = getLastSubmitAttempt(as);
        if (sa.isPresent() && sa.get().getTestAttempt() != null
                && !sa.get().getTestAttempt().getTestCases().isEmpty()) {
            long successTestCount = sa.get()
                    .getTestAttempt()
                    .getTestCases()
                    .stream()
                    .filter(TestCase::isSuccess)
                    .count();
            return Math.max(Math.round(initialScore * 0.05), 10) * successTestCount;
        }

        return 0L;
    }

    private Optional<SubmitAttempt> getLastSubmitAttempt(AssignmentStatus as) {
        return as.getSubmitAttempts()
                .stream()
                .max(Comparator.comparing(SubmitAttempt::getDateTimeStart));
    }

    private boolean isSuccessSubmit(AssignmentStatus as) {
        return getLastSubmitAttempt(as).map(SubmitAttempt::isSuccess).orElse(false);
    }

    private void registerScore(AssignmentStatus as, Score score) {
        AssignmentResult ar = as.getAssignmentResult();
        if (as.getAssignmentResult() == null) {
            ar = AssignmentResult.builder()
                    .assignmentStatus(as)
                    .uuid(UUID.randomUUID())
                    .build();
        }

        ar.setInitialScore(score.getInitialScore());
        ar.setBonus(score.getTotalBonus());
        ar.setPenalty(score.getTotalPenalty());
        ar.setFinalScore(score.getTotalScore());
        as.setAssignmentResult(ar);
        assignmentResultRepository.save(ar);

    }

    @Transactional
    public AssignmentStatus finalizeScore(AssignmentStatus as, ActiveAssignment activeAssignment) {
        // attach entity?
        as = assignmentStatusRepository.save(as);

        if (needsFinalize(as)) {

            Score score = calculateScore(activeAssignment, as);
            registerScore(as, score);

            // make sure cannot finalize twice.
            as.setDateTimeEnd(Instant.now());
            as = assignmentStatusRepository.save(as);

            log.info("Registered final score of {} composed of {} for team {} in assignment {}.", score.getTotalScore(), score, as
                    .getTeam()
                    .getName(), activeAssignment.getAssignment().getName());
        } else {
            return as;
        }
        return as;
    }

    private boolean needsFinalize(AssignmentStatus as) {
        return as.getDateTimeEnd() == null;
    }
}
