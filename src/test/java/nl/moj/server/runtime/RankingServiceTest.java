package nl.moj.server.runtime;

import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.rankings.service.RankingsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RankingServiceTest extends BaseRuntimeTest {

	@Autowired
	private RankingsService rankingsService;

	@Autowired
	private CompetitionRuntime competitionRuntime;

	@Test
	public void shouldGiveRankingsWithZeroScoreIfNoResultsAreFound() {
		// add some extra teams to trigger sorting, its smart and wont
		// do anything with only one team.
		addTeam();
		addTeam();

		List<Ranking> rankings = rankingsService.getRankings(competitionRuntime.getCompetitionSession());
		assertThat(rankings).isNotEmpty();
		rankings.forEach( r -> assertThat(r.getTotalScore()).isEqualTo(0));
	}
}
