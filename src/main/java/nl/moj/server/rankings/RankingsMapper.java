package nl.moj.server.rankings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import nl.moj.server.model.Ranking;
import nl.moj.server.repository.TeamRepository;

@Component
@RequiredArgsConstructor
public class RankingsMapper {

    private final TeamRepository teamRepository;

    public List<Ranking> getRankings() {
        List<Ranking> rankings = new ArrayList<>();

        teamRepository.findAll().forEach(t -> rankings.add(Ranking.builder()
                .team(t.getName())
                .totalScore(t.getTotalScore())
                .results(t.getResults())
                .build()));

        rankings.sort(Comparator.comparingInt(Ranking::getTotalScore));
        Collections.reverse(rankings);

        return rankings;
    }

}
