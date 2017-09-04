package nl.moj.server.model;

public class Result {

	private int id;
	
	private String team;

	private String assignment;

	private Integer score;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team= team;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}
	
	
	
}
