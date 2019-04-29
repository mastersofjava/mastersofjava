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
import nl.moj.server.test.model.TestCase;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
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
    private AssignmentStatus assignmentStatus;


    @Before
    public void before() {
        team = new Team();
        team.setId(1L);
        team.setName("Team 1");

        scoreService = new ScoreService(mojServerProperties, assignmentStatusRepository, assignmentResultRepository);

    }

    private ActiveAssignment prepareAssignmentStatus(Team team, Long initialScore, Integer testRuns,
                                                     Integer totalTestCases, Integer successTestCases,
                                                     Integer submits, boolean lastSubmitSuccess, ScoringRules scoringRules) {
        AssignmentDescriptor ad = new AssignmentDescriptor();
        ad.setScoringRules(scoringRules);

        List<TestAttempt> testAttempts = createTestAttempts(testRuns,totalTestCases,successTestCases);
        List<SubmitAttempt> submitAttempts = createSubmitAttempts(submits,lastSubmitSuccess,totalTestCases,successTestCases);

        submitAttempts.forEach( sa -> {
            testAttempts.add(sa.getTestAttempt());
        });

        assignmentStatus = AssignmentStatus.builder()
                .team(team)
                .testAttempts(testAttempts)
                .submitAttempts(submitAttempts)
                .build();
        setupGlobalSuccessBonus(500);

        return ActiveAssignment.builder()
                .assignment(new Assignment())
                .competitionSession(new CompetitionSession())
                .assignmentDescriptor(ad)
                .timeRemaining(initialScore)
                .build();
    }

    private List<SubmitAttempt> createSubmitAttempts(int count, boolean lastSubmitSuccess, int totalTestCases, int successTestCases ) {
        List<SubmitAttempt> sa = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            sa.add(SubmitAttempt.builder()
                    .assignmentStatus(assignmentStatus)
                    .dateTimeStart(now)
                    .dateTimeEnd(now.plusSeconds(45))
                    .success((i == count - 1) && lastSubmitSuccess )
                    .testAttempt(createTestAttempt(now,totalTestCases,successTestCases))
                    .build());
            now = now.plusSeconds(60);
        }
        return sa;
    }

    private TestAttempt createTestAttempt(Instant now, int totalTestCases, int successTestCases) {
        TestAttempt ta = new TestAttempt();
        ta.setAssignmentStatus(assignmentStatus);
        ta.setTestCases(new ArrayList<>());
        for( int j =0; j < totalTestCases; j++) {
            ta.getTestCases().add(TestCase.builder()
                    .testAttempt(ta)
                    .dateTimeStart(now.plusSeconds(j*3))
                    .name("test-"+j)
                    .success(j < successTestCases)
                    .build());
        }
        return ta;
    }

    private List<TestAttempt> createTestAttempts(int count, int totalTestCases, int successTestCases ) {
        List<TestAttempt> attempts = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            attempts.add(createTestAttempt(now.plusSeconds(i*60),totalTestCases,successTestCases));
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
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,1, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(0L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(3000L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(3000L);
    }

    @Test
    public void shouldUseGlobalSuccessBonus() {
        ScoringRules scoringRules = prepareScoringRules(null, null, null, null);

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,1, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2500L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void failedSubmitHasScore() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                2,2, false, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(0L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(200L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTotalBonus()).isEqualTo(200L);
        Assertions.assertThat(score.getSubmitBonus()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestBonus()).isEqualTo(200L);
    }

    @Test
    public void successSubmitHasScore() {
        ScoringRules scoringRules = prepareScoringRules(1, "500", 500, "100");

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                2,2, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getSubmitBonus()).isEqualTo(500L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(500L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(300L);
        Assertions.assertThat(score.getTestBonus()).isEqualTo(200L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(800L);
        Assertions.assertThat(score.getTotalBonus()).isEqualTo(700L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1900L);
    }

    // Submit Penalties

    @Test
    public void shouldHaveFixedResubmitPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,3, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(1000L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(1000L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void shouldHaveNoFixedResubmitPenaltyOnFirstSubmit() {
        ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,1, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void shouldHavePercentageResubmitPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, "25%", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,3, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1125L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(875L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(875L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }

    @Test
    public void shouldHaveNoPercentageResubmitPenaltyOnFirstSubmit() {
        ScoringRules scoringRules = prepareScoringRules(2, "25%", 0, null);
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,1, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }


    @Test
    public void invalidResubmitPenaltiesUsesZeroValue() {
        ScoringRules scoringRules = prepareScoringRules(2, "foo", 0, null);

        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,2, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

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
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 2, 2,
                0,2, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);
        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1900L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(100L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(100L);
    }

    @Test
    public void shouldHavePercentageTestPenalties() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "5%");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,1, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(1715L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(285L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(285L);
    }

    @Test
    public void invalidTestPenaltiesUsesZeroValue() {
        ScoringRules scoringRules = prepareScoringRules(2, null, 0, "foo");
        ActiveAssignment state = prepareAssignmentStatus(team, 2000L, 3, 2,
                0,2, true, scoringRules);

        Score score = scoreService.calculateScore(state, assignmentStatus);

        Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
        Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
        Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
    }
}
