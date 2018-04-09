package nl.moj.server.model;

import java.util.List;

public class Team {
	private int id;
	private String name;
	private String password;
	private String cpassword;
	private String role;
	private String country;
	private String company;
	
	private List<Result> results;
	
	public Team() {
	}
	
	public Team(String name, String role, String country, String company) {
		super();
		this.name = name;
		this.role = role;
		this.country = country;
		this.company = company;
	}

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
	
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public Integer getTotalScore(){
        int sum = 0;

	    if (getResults() != null) {
            for (Result r : getResults()) {
                sum += r.getScore();
            }
        }

		return sum;
	}

	public List<Result> getResults() {
		return results;
	}

	public void setResults(List<Result> results) {
		this.results = results;
	}

	public String getShortName() {
	    return name.length() > 20 ? name.substring(0, 20) + "..." : name;
    }
	
}
