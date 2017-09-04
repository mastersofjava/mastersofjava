package nl.moj.server.model;

import java.util.List;

public class Team {
	private int id;
	private String name;
	private String password;
	private String cpassword;
	private String role;
	private List<Result> results;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCpassword() {
		return cpassword;
	}

	public void setCpassword(String cpassword) {
		this.cpassword = cpassword;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
	
	public Integer getTotalScore(){
		int sum = 0;
		for(Result r : getResults()){
			sum += r.getScore();
		}
		return sum;
	}

	public List<Result> getResults() {
		return results;
	}

	public void setResults(List<Result> results) {
		this.results = results;
	}
//	public void addResult(Result result) {
//		this.results.add(result);
//	}
	
}
