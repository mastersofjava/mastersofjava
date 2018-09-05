package nl.moj.server.rankings;

import lombok.RequiredArgsConstructor;
import nl.moj.server.model.Ranking;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingsMapper {

    private final TeamRepository teamRepository;
    private final ResultRepository resultRepository;

    public List<Ranking> getRankings() {
        List<Ranking> rankings = new ArrayList<>();

        teamRepository.findAll().forEach(t -> rankings.add(Ranking.builder()
                .team(t.getName())
                //TODO fixme
                .totalScore(0)
                .results(new ArrayList<>())
                .build()));

        rankings.sort(Comparator.comparingInt(Ranking::getTotalScore));
        Collections.reverse(rankings);

        return rankings;
    }

}
