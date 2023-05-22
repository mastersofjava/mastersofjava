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
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import org.springframework.stereotype.Service;

/**
 * The ScoreService calculates the score.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreService {

    private final MojServerProperties mojServerProperties;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final AssignmentResultRepository assignmentResultRepository;

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
        ar.setScoreExplanation(score.getExplanation());
        as.setAssignmentResult(ar);
        as.setDateTimeCompleted(Instant.now());
        assignmentResultRepository.save(ar);

        return teamAssignmentStatusRepository.save(as);
    }

    private Score calculateScore(SubmitAttempt sa, TeamAssignmentStatus as, AssignmentDescriptor ad) {
    	Score score = new Score();
    	calculateSubmitBonus(score, sa, ad);
    	calculateInitialScore(score, sa);
        calculateTestBonus(score, sa, as, ad);
        calculateTestPenalty(score, sa, ad);
    	calculateSubmitPenalty(score, sa, ad);
    	return score;
    }

    /**
     * InitialScore: if success-submit then count time remaining (otherwise 0 )
     */
    private void calculateInitialScore(Score score, SubmitAttempt sa) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
        	score.setInitialScore(sa.getAssignmentTimeRemaining().toSeconds());
        	score.addExplanation("" + sa.getAssignmentTimeRemaining().toMinutes() +":" + sa.getAssignmentTimeRemaining().toSecondsPart() + " left after successful submission: " + score.getInitialScore() + " points" );
        } else {
        	score.setInitialScore(0L);
        	score.addExplanation("No successful submission bonus seconds: 0 points" );
        }
    }

    private void calculateSubmitPenalty(Score score,SubmitAttempt sa, AssignmentDescriptor ad) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
            TeamAssignmentStatus as = sa.getAssignmentStatus();
            int submits = as.countNotAbortedSubmitAttempts();
            if (submits > 1 && ad.getScoringRules().getResubmitPenalty() != null) {
                String penalty = ad.getScoringRules().getResubmitPenalty().trim();
                try {
                    // the first submit is always free, hence submits - 1.
                	StringBuilder explanation = new StringBuilder();
                    long finalPenalty = calculatePenaltyValue(score, score.getTotalScore(), submits - 1, penalty, "submit", explanation);
                    score.setResubmitPenalty(finalPenalty);
                    if (finalPenalty>0) {
                    	score.addExplanation(explanation.toString());
                    }
                    return;
                } catch (Exception nfe) {
                	throw new IllegalArgumentException("Cannot use submit penalty '"+penalty+"'. Expected a number or valid percentage.");
                }
            }
        }
        score.setResubmitPenalty(0L);
        
    }

    private void calculateTestPenalty(Score score, SubmitAttempt sa, AssignmentDescriptor ad) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
            TeamAssignmentStatus as = sa.getAssignmentStatus();
            // get the test run count, the first submit test is free, hence test runs - submit attempts
            int testRuns = as.getTestAttempts().size() - as.getSubmitAttempts().size();
            if (testRuns > 0 && ad.getScoringRules().getTestPenalty() != null) {
                String penalty = ad.getScoringRules().getTestPenalty().trim();
                try {
                	StringBuilder explanation = new StringBuilder();
                    long finalPenalty = calculatePenaltyValue(score, score.getTotalScore(), testRuns, penalty, "test", explanation);
                    score.setTestPenalty(finalPenalty);
                    if (finalPenalty>0) {
                    	score.addExplanation(explanation.toString());
                    }
                } catch (Exception nfe) {
                	throw new IllegalArgumentException("Cannot use test penalty '"+penalty+"'. Expected a number or valid percentage.");
                }
            }
        }
        
    }

    /**
     * Both for test runs and 
     * @param score the score object to record the penalty in
     * @param baseScore the base score to use for penalty calculation
     * @param count how many times to apply the penalty
     * @param penalty the name of the penalty
     * @param explanation a StringBuilder to append to explanation to
     * @throws NumberFormatException
     */
    private long calculatePenaltyValue(Score score, Long baseScore, Integer count, String penalty, String penaltyName, StringBuilder explanation) throws NumberFormatException {
    	if (count==0) {
    		explanation.append("No "+penaltyName+" penalty: 0 points");
    		return 0;
		} else {
	        if (penalty.endsWith("%") && count > 0) {
	            long p = 100L - Long.parseLong(penalty.substring(0, penalty.length() - 1));
	            if (p < 0) {
	                throw new IllegalArgumentException("Penalty percentage value must be <= 100%");
	            }
	            long finalPenalty = baseScore - Math.round(baseScore * Math.pow((p / 100.0), count.doubleValue()));
	            explanation.append("" + count + " "+penaltyName+"s, deducting " + penalty + " " + count + " times from base score of "+baseScore+": -" + finalPenalty + " points" );
	            return finalPenalty;
	        } else {
	            long p = Long.parseLong(penalty);
	            if (p < 0) {
	                throw new IllegalArgumentException("Penalty value must be >= 0.");
	            }
	            long finalPenalty = p * count;
	            explanation.append("" + count + " "+penaltyName+", deducting " + penalty + " " + count + " times from base score of "+baseScore+": -" + finalPenalty + " points" );
	            return finalPenalty;
	        }
    	}
    }

    /**
     * submitbonus: if success-submit then count success bonus (otherwise 0)
     */
    private void calculateSubmitBonus(Score score, SubmitAttempt sa, AssignmentDescriptor ad) {
        if (sa != null && sa.getSuccess() != null && sa.getSuccess()) {
            long submitBonus;
            if (ad.getScoringRules().getSuccessBonus() != null) {
                submitBonus = ad.getScoringRules().getSuccessBonus();
            } else {
                submitBonus = mojServerProperties.getCompetition().getSuccessBonus();
            }
            score.setSubmitBonus(submitBonus);
            score.addExplanation("Successful submit: " + submitBonus + " points");
        } else {
        	score.setSubmitBonus(0L);
        	score.addExplanation("No successful submit: 0 points");
        }
    }

    /**
     * Test bonus is always applied and based on the unique set of tests
     * completed over all test attempts and submit attempts.
     * - default: timeRemaining * 0.05 * succesvolle tests.
     * - per individuele test: volgens geconfigureerde score.
     */
    private void calculateTestBonus(Score score, SubmitAttempt sa, TeamAssignmentStatus as, AssignmentDescriptor ad) {
        Set<String> succeededTestCases = Optional.ofNullable(as.getTestAttempts())
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(ta -> ta.getTestCases().stream())
                .filter(tc -> tc.getSuccess() != null && tc.getSuccess())
                .map(tc -> tc.getName().toLowerCase())
                .collect(Collectors.toSet());

        if (!succeededTestCases.isEmpty()) {
            boolean useDefaultBonus = ad.getLabels().stream().noneMatch(l -> l.startsWith("test"));
            if (useDefaultBonus) {
            	score.setTestBonus((long)succeededTestCases.size());
            	score.addExplanation("Mini bonus for "+succeededTestCases.size()+" successful tests:" + score.getTestBonus() + " points");
            } else {
            	calculateTestBonusViaConfiguration(score, succeededTestCases, ad);
            }
        } else {
        	score.setTestBonus(0L);
        	score.addExplanation("No bonus for "+succeededTestCases.size()+" successful tests: 0 points");
        }
    }

    private void calculateTestBonusViaConfiguration(Score score, Set<String> succeededTestCases, AssignmentDescriptor configuration) {
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
            	score.addExplanation("Test "+testCase+" success bonus: " + configDetails.get(key) + " points");
            }
        }
    	score.setTestBonus(sum);
    }
}
