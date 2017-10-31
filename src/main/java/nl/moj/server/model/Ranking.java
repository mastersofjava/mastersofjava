package nl.moj.server.model;

import java.util.List;

public class Ranking {

	private Integer rank;
	
	private String team;

	private List<Result> results;

	private Integer totalScore;
	
	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public List<Result> getResults() {
		return results;
	}

	public void setResults(List<Result> results) {
		this.results = results;
	}

	public Integer getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(Integer totalScore) {
		this.totalScore = totalScore;
	}

	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

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
