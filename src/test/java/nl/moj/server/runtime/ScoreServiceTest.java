package nl.moj.server.runtime;

import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.descriptor.ScoringRules;
import nl.moj.server.config.properties.Competition;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.runtime.model.TeamStatus;
import nl.moj.server.teams.model.Team;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ScoreServiceTest {

	@Mock
	private ResultRepository resultRepository;

	@Mock
	private MojServerProperties mojServerProperties;

	private ScoreService scoreService;
	private Team team;


	@Before
	public void before() {
		team = new Team();
		team.setId(1L);
		team.setName("Team 1");

		scoreService = new ScoreService(resultRepository,mojServerProperties);
	}

	private AssignmentState prepareAssignmentStatus(Team team, Long initialScore, Integer testRuns, Integer submits, ScoringRules scoringRules) {
		AssignmentDescriptor ad = new AssignmentDescriptor();
		ad.setScoringRules(scoringRules);

		Map<Team, TeamStatus> teamStatuses = new HashMap<>();
		teamStatuses.put(team, TeamStatus.init(team)
				.toBuilder()
				.submits(submits)
				.testRuns(testRuns)
				.build());

		setupGlobalSuccessBonus(500);

		return AssignmentState.builder()
				.assignmentDescriptor(ad)
				.teamStatuses(teamStatuses)
				.timeRemaining(initialScore)
				.build();
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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 3, 1, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 0, 3, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 0, 3, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 0, 1, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 0, 3, scoringRules);

		Score score = scoreService.calculateScore(team, state, true);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
	}

	// Test Penalties

	@Test
	public void shouldHaveFixedTestPenalties() {
		ScoringRules scoringRules = prepareScoringRules(2, null, 0, "50");
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 2, 1, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 3, 1, scoringRules);

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
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 3, 1, scoringRules);

		Score score = scoreService.calculateScore(team, state, true);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getTestPenalty()).isEqualTo(0L);
	}
}
