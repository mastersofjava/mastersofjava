package nl.moj.server.rankings.service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.teams.model.Role;
import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingsService {

	private final TeamRepository teamRepository;
	private final ResultRepository resultRepository;

	public List<Ranking> getRankings(CompetitionSession session) {
		List<Ranking> rankings = new ArrayList<>();

		if( session != null ) {
			teamRepository.findAllByRole(Role.ROLE_USER).forEach(t -> rankings.add(Ranking.builder()
					.team(t.getName())
					.totalScore(resultRepository.getTotalScore(t, session))
					.results(resultRepository.findAllByTeamAndCompetitionSession(t, session))
					.build()));

			rankings.sort(Comparator.comparingInt(Ranking::getTotalScore));
			Collections.reverse(rankings);
		}
		return rankings;
	}
}
