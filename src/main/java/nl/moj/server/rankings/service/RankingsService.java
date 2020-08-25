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
package nl.moj.server.rankings.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.rankings.model.RankingHeader;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.CompetitionState;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingsService {

    private final TeamRepository teamRepository;
    private final AssignmentService assignmentService;
    private final AssignmentResultRepository assignmentResultRepository;

    public List<Ranking> getRankings(CompetitionSession session) {
        return getRankings(session, null);
    }
    public List<Ranking> getRankings(CompetitionSession session, String selectedYearFilter) {
        List<Ranking> rankings = new ArrayList<>();
        if (session != null) {
            final List<AssignmentResult> sessionList = assignmentResultRepository.findByCompetitionSession(session);
            final List<AssignmentResult> assignmentResultsPerYear = new ArrayList<>();

            for (AssignmentResult result: sessionList) {
                boolean isInsertAssignment = selectedYearFilter==null || result.getAssignmentStatus().getAssignment().getAssignmentDescriptor().contains(selectedYearFilter);

                if (isInsertAssignment) {
                    assignmentResultsPerYear.add(result);
                }
            }
            teamRepository.findAll().forEach(team -> {
                Ranking rank = Ranking.builder()
                        .team(team.getName())
                        .totalScore(calculateTotalScore(assignmentResultsPerYear, team))
                        .build();
                getTeamAssignments(assignmentResultsPerYear, team).forEach(ar -> {
                    Assignment assignment = ar.getAssignmentStatus().getAssignment();
                    rank.addAssignmentScore(assignment, ar.getFinalScore());
                });
                rankings.add(rank);
            });

            rankings.sort(Comparator.comparingLong(Ranking::getTotalScore));
            Collections.reverse(rankings);
        }
        return rankings;
    }

    public List<RankingHeader> getRankingHeaders(CompetitionState competitionState) {
        return competitionState.getCompletedAssignments()
                .stream()
                .map(oa -> RankingHeader.builder()
                        .orderedAssignment(oa)
                        .displayName(assignmentService.getAssignmentDescriptor(oa.getAssignment()).getDisplayName())
                        .build())
                .collect(Collectors.toList());

    }

    private Stream<AssignmentResult> getTeamAssignments(List<AssignmentResult> assignmentResults, Team t) {
        return assignmentResults.stream().filter(ar -> ar.getAssignmentStatus().getTeam().equals(t));
    }

    private Long calculateTotalScore(List<AssignmentResult> assignmentResults, Team t) {
        return getTeamAssignments(assignmentResults, t)
                .map(AssignmentResult::getFinalScore)
                .reduce(0L, Long::sum);
    }
}
