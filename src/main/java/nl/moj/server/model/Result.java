package nl.moj.server.model;

public class Result {

	private int id;
	
	private String team;

	private String assignment;

	private Integer score;
	
	private Integer penalty;

	private Integer credit;
	
	public Result() {
	}

	public Result(String team, String assignment, Integer score, Integer penalty, Integer credit) {
		super();
		this.team = team;
		this.assignment = assignment;
		this.score = score;
		this.penalty = penalty;
		this.credit = credit;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public Integer getPenalty() {
		return penalty;
	}

	public void setPenalty(Integer penalty) {
		this.penalty = penalty;
	}

	public Integer getCredit() {
		return credit;
	}

	public void setCredit(Integer credit) {
		this.credit = credit;
	}
	
	
	
	
}
