package nl.moj.server.rankings.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.OrderedAssignment;

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

    public AssignmentScore getAssignmentResult(OrderedAssignment oa) {
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
