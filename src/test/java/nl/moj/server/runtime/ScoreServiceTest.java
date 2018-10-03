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

	private AssignmentState prepareAssignmentStatus(Team team, Long initialScore, Integer submits, ScoringRules scoringRules) {
		AssignmentDescriptor ad = new AssignmentDescriptor();
		ad.setScoringRules(scoringRules);

		Map<Team, TeamStatus> teamStatuses = new HashMap<>();
		teamStatuses.put(team, TeamStatus.init(team)
				.toBuilder().submits(submits).build());

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

	private ScoringRules prepareScoringRules(int maxResubmits, String resubmitPenalty, int successBonus, String testPenalty) {
		ScoringRules scoringRules = new ScoringRules();
		scoringRules.setMaximumResubmits(maxResubmits);
		scoringRules.setResubmitPenalty(resubmitPenalty);
		scoringRules.setSuccessBonus(successBonus);
		scoringRules.setTestPenalty(testPenalty);
		return scoringRules;
	}

	@Test
	public void failedSubmitHasZeroScore() {
		ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 1, scoringRules);

		Score score = scoreService.calculateScore(team, state, false);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(0L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(0L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
	}
	
	@Test
	public void shouldHaveFixedResubmitPenalties() {
		ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 3, scoringRules);

		Score score = scoreService.calculateScore(team, state, true);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(1000L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(1000L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(1000L);
	}

	@Test
	public void shouldHaveNoFixedResubmitPenaltyOnFirstSubmit() {
		ScoringRules scoringRules = prepareScoringRules(2, "500", 0, null);
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 1, scoringRules);

		Score score = scoreService.calculateScore(team, state, true);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
	}

	@Test
	public void shouldHavePercentageResubmitPenalties() {
		ScoringRules scoringRules = prepareScoringRules(2, "50%", 0, null);
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 3, scoringRules);

		Score score = scoreService.calculateScore(team, state, true);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(500L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(1500L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(1500L);
	}

	@Test
	public void shouldHaveNoPercentageResubmitPenaltyOnFirstSubmit() {
		ScoringRules scoringRules = prepareScoringRules(2, "50%", 0, null);
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 1, scoringRules);

		Score score = scoreService.calculateScore(team, state, true);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
	}


	@Test
	public void invalidResubmitPenaltiesUsesZeroValue() {
		ScoringRules scoringRules = prepareScoringRules(2, "foo", 0, null);
		AssignmentState state = prepareAssignmentStatus(team, 2000L, 3, scoringRules);

		Score score = scoreService.calculateScore(team, state, true);

		Assertions.assertThat(score.getInitialScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalScore()).isEqualTo(2000L);
		Assertions.assertThat(score.getTotalPenalty()).isEqualTo(0L);
		Assertions.assertThat(score.getResubmitPenalty()).isEqualTo(0L);
	}


}
