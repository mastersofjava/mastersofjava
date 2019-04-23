package nl.moj.server.runtime;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.descriptor.ScoringRules;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.config.properties.Competition;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.isA;

@RunWith(MockitoJUnitRunner.class)
public class ScoreServiceTest {

    @Mock
    private MojServerProperties mojServerProperties;

    @Mock
    private AssignmentStatusRepository assignmentStatusRepository;

    @Mock
    private AssignmentResultRepository assignmentResultRepository;

    private ScoreService scoreService;
    private Team team;


    @Before
    public void before() {
        team = new Team();
        team.setId(1L);
        team.setName("Team 1");

        scoreService = new ScoreService(mojServerProperties, assignmentStatusRepository, assignmentResultRepository);
    }

    private ActiveAssignment prepareAssignmentStatus(Team team, Long initialScore, Integer testRuns, Integer submits, ScoringRules scoringRules) {
        AssignmentDescriptor ad = new AssignmentDescriptor();
        ad.setScoringRules(scoringRules);

        AssignmentStatus as = AssignmentStatus.builder()
                .team(team)
                .testAttempts(createTestAttempts(testRuns))
                .submitAttempts(createSubmitAttempts(submits))
                .build();

        Mockito.when(assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(isA(Assignment.class), isA(CompetitionSession.class), isA(Team.class)))
                .thenReturn(as);
        setupGlobalSuccessBonus(500);

        return ActiveAssignment.builder()
                .assignment(new Assignment())
                .competitionSession(new CompetitionSession())
                .assignmentDescriptor(ad)
                .timeRemaining(initialScore)
                .build();
    }

    private List<SubmitAttempt> createSubmitAttempts(int count) {
        List<SubmitAttempt> sa = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sa.add(new SubmitAttempt());
        }
        return sa;
    }

    private List<TestAttempt> createTestAttempts(int count) {
        List<TestAttempt> sa = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sa.add(new TestAttempt());
        }
        return sa;
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
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(0L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(3000L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(3000L);
    }

    @Test
    public void shouldUseGlobalSuccessBonus() {
        ScoringRules scoringRules = prepareScoringRules(null, null, null, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2500L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void failedSubmitHasZeroScore() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, false);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(0L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(0L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    // Submit Penalties

    @Test
    public void shouldHaveFixedResubmitPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 0, 3, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(1000L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(1000L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void shouldHaveNoFixedResubmitPenaltyOnFirstSubmit() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void shouldHavePercentageResubmitPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, "25%", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 0, 3, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1125L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(875L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(875L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void shouldHaveNoPercentageResubmitPenaltyOnFirstSubmit() {
        ScoringRules scoringRules = prepareScoringRules(2, "25%", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }


    @Test
    public void invalidResubmitPenaltiesUsesZeroValue() {
        ScoringRules scoringRules = prepareScoringRules(2, "foo", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 0, 3, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    // TestCase Penalties

    @Test
    public void shouldHaveFixedTestPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "50");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 2, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1900L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(100L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(100L);
    }

    @Test
    public void shouldHavePercentageTestPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "5%");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1715L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(285L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(285L);
    }

    @Test
    public void invalidTestPenaltiesUsesZeroValue() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "foo");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 1, scoringRules);

        Score score = scoreService.calculateScore(team, state, true);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }
}
