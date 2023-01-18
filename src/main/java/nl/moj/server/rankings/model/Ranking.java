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
package nl.moj.server.rankings.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionAssignment;

@Data
@Builder
public class Ranking {

    private Integer rank;
    private String team;

    @Builder.Default
    private Long totalScore = 0L;

    @Builder.Default
    private List<AssignmentScore> assignmentScores = new ArrayList<>();

    public String getResultJson() {
        StringBuffer b = new StringBuffer();
        b.append("{\"scores\":[");
        assignmentScores.forEach(r -> {
            b.append("{\"name\":\"")
                    .append(r.getAssignment())
                    .append("\",\"score\":")
                    .append(r.getScore())
                    .append("},");
        });
        if (b.length() > 1) {
            b.deleteCharAt(b.length() - 1);
        }
        b.append("]}");
        return b.toString();
    }

    public AssignmentScore getAssignmentResult(CompetitionAssignment oa) {
        return assignmentScores.stream()
                .filter(r -> r.getAssignmentUuid().equals(oa.getAssignment().getUuid()))
                .findFirst()
                .orElse(
                        emptyResult(oa.getAssignment()));
    }

    private AssignmentScore emptyResult(Assignment a) {
        return new AssignmentScore(a.getUuid(), a.getName(), 0L);
    }

    public void addAssignmentScore(Assignment assignment, Long finalScore) {
        assignmentScores.add(new AssignmentScore(assignment.getUuid(), assignment.getName(), finalScore));
    }

    @Getter
    @AllArgsConstructor
    private class AssignmentScore {
        private UUID assignmentUuid;
        private String assignment;
        private Long score;
    }
}
