package nl.moj.server.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

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
}
