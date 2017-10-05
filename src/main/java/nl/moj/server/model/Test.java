package nl.moj.server.model;

public class Test {

	private String team;
	private String assignment;
	private String testname;
	private Integer success;
	private Integer failure;

	public Test(String team, String assignment, String testname, Integer success, Integer failure) {
		super();
		this.team = team;
		this.assignment = assignment;
		this.testname = testname;
		this.success = success;
		this.failure = failure;
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

	public String getTestname() {
		return testname;
	}

	public void setTestname(String testname) {
		this.testname = testname;
	}

	public Integer getSuccess() {
		return success;
	}

	public void setSuccess(Integer success) {
		this.success = success;
	}

	public Integer getFailure() {
		return failure;
	}

	public void setFailure(Integer failure) {
		this.failure = failure;
	}
}
