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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.assignment.descriptor.ScoringRules;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.common.config.properties.Competition;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ScoreServiceTest {

    @Mock(lenient = true)
    private MojServerProperties mojServerProperties;

    @Mock(lenient = true)
    private TeamAssignmentStatusRepository teamAssignmentStatusRepository;

    @Mock(lenient = true)
    private AssignmentResultRepository assignmentResultRepository;

    private ScoreService scoreService;
    private Team team;
    private TeamAssignmentStatus assignmentStatus;

    @BeforeEach
    public void before() {
        team = new Team();
        team.setId(1L);
        team.setName("Team 1");
        Mockito.when(teamAssignmentStatusRepository.save(Mockito.any(TeamAssignmentStatus.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        Mockito.when(assignmentResultRepository.save(Mockito.any(AssignmentResult.class))).thenAnswer( invocationOnMock -> invocationOnMock.getArgument(0));
        scoreService = new ScoreService(mojServerProperties, teamAssignmentStatusRepository, assignmentResultRepository);

    }

    private ActiveAssignment prepareAssignmentStatus(Team team, Long initialScore, Integer testRuns,
                                                     List<String> totalTestCases, Integer successTestCases,
                                                     Integer submits, boolean lastSubmitSuccess, ScoringRules scoringRules) {
        return prepareAssignmentStatus(team, Collections.emptyList(), initialScore, testRuns, totalTestCases, successTestCases,
                submits, lastSubmitSuccess, scoringRules);
    }

    private ActiveAssignment prepareAssignmentStatus(Team team, List<String> labels, Long initialScore, Integer testRuns,
                                                     List<String> totalTestCases, Integer successTestCases,
                                                     Integer submits, boolean lastSubmitSuccess, ScoringRules scoringRules) {
        assignmentStatus = TeamAssignmentStatus.builder()
                .team(team)
                .build();

        AssignmentDescriptor ad = new AssignmentDescriptor();
        ad.setScoringRules(scoringRules);
        ad.setLabels(labels);
        List<TestAttempt> testAttempts = createTestAttempts(testRuns, totalTestCases, successTestCases);
        List<SubmitAttempt> submitAttempts = createSubmitAttempts(submits, lastSubmitSuccess, initialScore,totalTestCases, successTestCases);

        submitAttempts.forEach(sa -> {
            sa.setAssignmentStatus(assignmentStatus);
            testAttempts.add(sa.getTestAttempt());
        });

        testAttempts.forEach(ta -> {
            ta.setAssignmentStatus(assignmentStatus);
        });

        assignmentStatus.setTestAttempts(testAttempts);
        assignmentStatus.setSubmitAttempts(submitAttempts);
        setupGlobalSuccessBonus(500);

        Mockito.when(teamAssignmentStatusRepository.save(any(TeamAssignmentStatus.class))).thenAnswer(i -> i.getArgument(0));

        return ActiveAssignment.builder()
                .assignment(new Assignment())
                .competitionSession(new CompetitionSession())
                .assignmentDescriptor(ad)
                .secondsRemaining(initialScore)
                .build();
    }

    private List<SubmitAttempt> createSubmitAttempts(int count, boolean lastSubmitSuccess, long timeRemaining, List<String> totalTestCases, int successTestCases) {
        List<SubmitAttempt> sa = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            TestAttempt ta = createTestAttempt(now, totalTestCases, successTestCases);
            sa.add(SubmitAttempt.builder()
                    .assignmentStatus(assignmentStatus)
                    .dateTimeRegister(now)
                    .dateTimeStart(ta.getDateTimeStart())
                    .dateTimeEnd(ta.getDateTimeEnd())
                    .assignmentTimeRemaining(Duration.ofSeconds(timeRemaining))
                    .success((i == count - 1) && lastSubmitSuccess)
                    .testAttempt(ta)
                    .build());
            now = now.plusSeconds(60);
        }
        return sa;
    }

    private CompileAttempt createCompileAttempt(Instant now, boolean success) {
        return CompileAttempt.builder()
                .dateTimeRegister(now)
                .dateTimeStart(now.plusSeconds(2))
                .dateTimeEnd(now.plusSeconds(3))
                .worker("worker1")
                .trace("trace")
                .success(success)
                .aborted(false)
                .timeout(false)
                .compilerOutput(success ? "OK" : "NOK")
                .build();
    }

    private TestAttempt createTestAttempt(Instant now, List<String> totalTestCases, int successTestCases) {
        CompileAttempt ca = createCompileAttempt(now, true);
        Instant ts = ca.getDateTimeEnd().plusSeconds(1);
        TestAttempt ta = new TestAttempt();
        ta.setCompileAttempt(ca);
        ta.setDateTimeRegister(now);
        ta.setDateTimeStart(ts);
        ta.setAssignmentStatus(assignmentStatus);
        ta.setTestCases(new ArrayList<>());
        for (int j = 0; j < totalTestCases.size(); j++) {
            ta.getTestCases().add(TestCase.builder()
                    .testAttempt(ta)
                    .dateTimeRegister(now)
                    .dateTimeStart(ts.plusSeconds(j * 3L))
                    .dateTimeEnd(ts.plusSeconds(j * 3L + 1L))
                    .name(totalTestCases.get(j))
                    .success(j < successTestCases)
                    .build());
        }
        ta.setDateTimeEnd(ta.getTestCases().get(ta.getTestCases().size() - 1).getDateTimeEnd());
        return ta;
    }

    private List<TestAttempt> createTestAttempts(int count, List<String> totalTestCases, int successTestCases) {
        List<TestAttempt> attempts = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            attempts.add(createTestAttempt(now.plusSeconds(i * 60L), totalTestCases, successTestCases));
        }
        return attempts;
    }

    private void setupGlobalSuccessBonus(int successBonus) {
        Competition c = new Competition();
        c.setSuccessBonus(successBonus);
        Mockito.when(mojServerProperties.getCompetition()).thenReturn(c);
    }

    private ScoringRules prepareScoringRules(Integer maxResubmits, String resubmitPenalty, Integer successBonus, String testPenalty) {
        ScoringRules scoringRules = new ScoringRules();
        scoringRules.setMaximumResubmits(maxResubmits);
        scoringRules.setResubmitPenalty(resubmitPenalty);
        scoringRules.setSuccessBonus(successBonus);
        scoringRules.setTestPenalty(testPenalty);
        return scoringRules;
    }

    @Test
    public void scoreShouldNotGoBelowZero() {
        ScoringRules scoringRules = prepareScoringRules(null, null, 0, "1000");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 1, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(0L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(3000L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(0L);
    }

    @Test
    public void shouldUseGlobalSuccessBonus() {
        ScoringRules scoringRules = prepareScoringRules(null, null, null, null);

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 1, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(2500L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(0L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(500L);
    }

    @Test
    public void failedSubmitHasScore() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                2, 2, false, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();
        System.out.println(ar.getScoreExplanation());

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(0L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(2L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(0L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(2L);
    }

    @Test
    public void successSubmitHasScore() {
        ScoringRules scoringRules = prepareScoringRules(1, "500", 500, "100");

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                2, 2, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();
        
        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(1702L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(800L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(502L);
    }

    @Test
    public void failedSubmitHasTestSuccessScoresWithAllTestsOK() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);

        ActiveAssignment state = prepareAssignmentStatus(team, List.of("test1_75", "test2_150"), 2000L, 1, List.of("Test1.java", "Test2.java"),
                2, 1, false, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(0L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(225L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(0L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(225L);
    }

    @Test
    public void successSubmitHasTestSuccessScoresWithAllTestsOK() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);

        ActiveAssignment state = prepareAssignmentStatus(team, List.of("test1_75", "test2_150"), 2000L, 1, List.of("Test1.java", "Test2.java"),
                2, 1, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(2225L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(0L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(225L);
    }

    @Test
    public void failedSubmitHasTestSuccessScores() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);

        ActiveAssignment state = prepareAssignmentStatus(team, List.of("test1_75", "test2_150"), 2000L, 1, List.of("Test1.java", "Test2.java"),
                1, 1, false, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(0L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(75L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(0L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(75L);
    }

    // Submit Penalties

    @Test
    public void shouldHaveFixedResubmitPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 3, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(1000L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(1000L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(0L);
    }

    @Test
    public void shouldHaveNoFixedResubmitPenaltyOnFirstSubmit() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 1, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(0L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(0L);
    }

    @Test
    public void shouldHavePercentageResubmitPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, "25%", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 3, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(1125L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(875L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(0L);
    }

    @Test
    public void shouldHaveNoPercentageResubmitPenaltyOnFirstSubmit() {
        ScoringRules scoringRules = prepareScoringRules(2, "25%", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 1, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(0L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(0L);
    }


    @Test
    public void invalidResubmitPenaltiesThrowsException() {
        ScoringRules scoringRules = prepareScoringRules(2, "foo", 0, null);

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 2, true, scoringRules);

        assertThrows(IllegalArgumentException.class, () -> scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor()));
    }

    // TestCase Penalties

    @Test
    public void shouldHaveFixedTestPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "50");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 2, List.of("test1", "test2"),
                0, 2, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(1900L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(100L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(0L);
    }

    @Test
    public void shouldHavePercentageTestPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "5%");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 1, true, scoringRules);

        TeamAssignmentStatus as = scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor());
        AssignmentResult ar = as.getAssignmentResult();

        Assertions.assertThat(ar).isNotNull();
        Assertions.assertThat(ar.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(ar.getFinalScore()).isEqualTo(1715L);
        Assertions.assertThat(ar.getPenalty()).isEqualTo(285L);
        Assertions.assertThat(ar.getBonus()).isEqualTo(0L);
    }

    @Test
    public void invalidTestPenaltiesThrowsException() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "foo");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, List.of("test1", "test2"),
                0, 2, true, scoringRules);

        assertThrows(IllegalArgumentException.class, () -> scoreService.finalizeScore(assignmentStatus.getMostRecentSubmitAttempt(), state.getAssignmentDescriptor()));
    }
}
