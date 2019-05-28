package nl.moj.server.rankings.service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class RankingsService {

    private final TeamRepository teamRepository;
    private final AssignmentResultRepository assignmentResultRepository;

    public List<Ranking> getRankings(CompetitionSession session) {
        List<Ranking> rankings = new ArrayList<>();
        if (session != null) {
            List<AssignmentResult> assignmentResults = assignmentResultRepository.findByCompetitionSession(session);
            teamRepository.findAllByRole(Role.USER).forEach(t -> {
                Ranking rank = Ranking.builder()
                        .team(t.getName())
                        .totalScore(calculateTotalScore(assignmentResults, t))
                        .build();
                getTeamAssignments(assignmentResults,t).forEach(ar -> {
                    rank.addAssignmentScore(ar.getAssignmentStatus().getAssignment(),ar.getFinalScore());
                });
                rankings.add(rank);
            });

            rankings.sort(Comparator.comparingLong(Ranking::getTotalScore));
            Collections.reverse(rankings);
        }
        return rankings;
    }

    private Stream<AssignmentResult> getTeamAssignments(List<AssignmentResult> assignmentResults, Team t) {
        return assignmentResults.stream().filter( ar -> ar.getAssignmentStatus().getTeam().equals(t));
    }

    private Long calculateTotalScore(List<AssignmentResult> assignmentResults, Team t) {
        return getTeamAssignments(assignmentResults,t)
                .map(AssignmentResult::getFinalScore)
                .reduce(0L, Long::sum);
    }
}
