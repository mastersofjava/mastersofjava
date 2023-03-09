/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.runtime;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
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
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final AssignmentResultRepository assignmentResultRepository;

//    public void initializeScoreAtStart(AssignmentStatus as) {
//        AssignmentResult ar = assignmentResultRepository.save(AssignmentResult.builder()
//                .assignmentStatus(as)
//                .bonus(0L)
//                .penalty(0L)
//                .initialScore(0L)
//                .finalScore(0L)
//                .uuid(UUID.randomUUID())
//                .build());
//        as.setAssignmentResult(ar);
//    }

    @Transactional(Transactional.TxType.MANDATORY)
    public TeamAssignmentStatus finalizeScore(SubmitAttempt sa, AssignmentDescriptor ad) {
        return finalizeScore(sa, sa.getAssignmentStatus(), ad);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public TeamAssignmentStatus finalizeScore(TeamAssignmentStatus as, AssignmentDescriptor ad) {
        return finalizeScore(null, as, ad);
    }

    private TeamAssignmentStatus finalizeScore(SubmitAttempt sa, TeamAssignmentStatus as, AssignmentDescriptor ad) {
        Score score = calculateScore(sa, as, ad);
        AssignmentResult ar = as.getAssignmentResult();
        if (ar == null) {
            ar = AssignmentResult.builder()
                    .assignmentStatus(as)
                    .build();
        }
        ar.setInitialScore(score.getInitialScore());
        ar.setBonus(score.getTotalBonus());
        ar.setPenalty(score.getTotalPenalty());
        ar.setFinalScore(score.getTotalScore());
        as.setAssignmentResult(ar);
        as.setDateTimeCompleted(Instant.now());
        assignmentResultRepository.save(ar);

        return teamAssignmentStatusRepository.save(as);
    }

    private Score calculateScore(SubmitAttempt sa, TeamAssignmentStatus as, AssignmentDescriptor ad) {
        return Score.builder()
                .initialScore(calculateInitialScore(sa))
                .submitBonus(calculateSubmitBonus(sa, ad))
                .testBonus(calculateTestBonus(sa, as, ad))
                .resubmitPenalty(calculateSubmitPenalty(sa, ad))
                .testPenalty(calculateTestPenalty(sa, ad))
                .build();
    }

    /**
     * InitialScore: if success-submit then count time remaining (otherwise 0 )
     */
    private Long calculateInitialScore(SubmitAttempt sa) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
            return sa.getAssignmentTimeRemaining().toSeconds();
        }
        return 0L;
    }

    private Long calculateSubmitPenalty(SubmitAttempt sa, AssignmentDescriptor ad) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
            TeamAssignmentStatus as = sa.getAssignmentStatus();
            int submits = as.getSubmitAttempts().size();
            if (submits > 1 && ad.getScoringRules().getResubmitPenalty() != null) {
                String penalty = ad.getScoringRules().getResubmitPenalty().trim();
                try {
                    // the first submit is always free, hence submits - 1.
                    return calculatePenaltyValue(sa.getAssignmentTimeRemaining().toSeconds(), submits - 1, penalty);
                } catch (Exception nfe) {
                    log.warn("Cannot use submit penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty);
                    log.trace("Cannot use submit penalty from '{}'. Expected a number or valid percentage, ignoring and using a value of 0.", penalty, nfe);
                }
            }
        }
        return 0L;
    }

    private Long calculateTestPenalty(SubmitAttempt sa, AssignmentDescriptor ad) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
            TeamAssignmentStatus as = sa.getAssignmentStatus();
            // get the test run count, the first submit test is free, hence test runs - submit attempts
            int testRuns = as.getTestAttempts().size() - as.getSubmitAttempts().size();
            if (testRuns > 0 && ad.getScoringRules().getTestPenalty() != null) {
                String penalty = ad.getScoringRules().getTestPenalty().trim();
                try {
                    return calculatePenaltyValue(sa.getAssignmentTimeRemaining().toSeconds(), testRuns, penalty);
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
            long p = 100L - Long.parseLong(penalty.substring(0, penalty.length() - 1));
            if (p < 0) {
                throw new IllegalArgumentException("Penalty percentage value must be <= 100%");
            }
            return initialScore - Math.round(initialScore * Math.pow((p / 100.0), count.doubleValue()));
        } else {
            long p = Long.parseLong(penalty);
            if (p < 0) {
                throw new IllegalArgumentException("Penalty value must be >= 0.");
            }
            return p * count;
        }
    }

    /**
     * submitbonus: if success-submit then count success bonus (otherwise 0)
     */
    private long calculateSubmitBonus(SubmitAttempt sa, AssignmentDescriptor ad) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
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

    /**
     * Test bonus is always applied and based on the unique set of tests
     * completed over all test attempts and submit attempts.
     * - default: timeRemaining * 0.05 * succesvolle tests.
     * - per individuele test: volgens geconfigureerde score.
     */
    private long calculateTestBonus(SubmitAttempt sa, TeamAssignmentStatus as, AssignmentDescriptor ad) {
        Set<String> succeededTestCases = Optional.ofNullable(as.getTestAttempts())
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(ta -> ta.getTestCases().stream())
                .filter(tc -> tc.getSuccess() != null && tc.getSuccess())
                .map(tc -> tc.getName().toLowerCase())
                .collect(Collectors.toSet());

        long defBonus = 0L;
        if (sa != null) {
            defBonus = Math.round(sa.getAssignmentTimeRemaining().toSeconds() * 0.05);
        }
        if (!succeededTestCases.isEmpty()) {
            boolean useDefaultBonus = ad.getLabels().stream().noneMatch(l -> l.startsWith("test"));
            if (useDefaultBonus) {
                return Math.max(defBonus, 10) * succeededTestCases.size();
            }
            return calculateTestBonusViaConfiguration(succeededTestCases, ad);
        }
        return 0L;
    }

    private long calculateTestBonusViaConfiguration(Set<String> succeededTestCases, AssignmentDescriptor configuration) {
        Map<String, Integer> configDetails = new LinkedHashMap<>();
        long sum = 0;
        for (String label : configuration.getLabels()) {
            if (label.startsWith("test") && label.contains("_")) {
                String[] parts = label.split("_");
                configDetails.put(parts[0].toLowerCase(), Integer.parseInt(parts[1]));
            }
        }

        for (String testCase : succeededTestCases) {
            String key = testCase.replace(".java", "").toLowerCase();
            if (configDetails.containsKey(key)) {
                sum += configDetails.get(key);
            }
        }
        return sum;
    }
}
