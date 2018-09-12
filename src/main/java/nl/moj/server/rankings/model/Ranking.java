package nl.moj.server.rankings.model;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.runtime.model.Result;

import java.util.List;

@Data
@Builder
public class Ranking {

    private Integer rank;

    private String team;

    private List<Result> results;

    private Integer totalScore;

    public String getResultJson() {
        StringBuffer b = new StringBuffer();
        b.append("{\"scores\":[");
        results.forEach( r -> {
            b.append("{\"name\":\"").append(r.getAssignment()).append("\",\"score\":").append(r.getScore()).append("},");
        });
        if( b.length() > 1) {
            b.deleteCharAt(b.length() - 1);
        }
        b.append("]}");
        return b.toString();
    }

    public Result getAssignmentResult(OrderedAssignment oa) {
        return results.stream().filter( r -> r.getAssignment().equals(oa.getAssignment())).findFirst().orElse(
        emptyResult(oa.getAssignment()));
    }

    private Result emptyResult(Assignment a) {
        Result r = new Result();
        r.setAssignment(a);
        r.setScore(0);
        r.setPenalty(0);
        r.setCredit(0);
        return r;
    }
}
