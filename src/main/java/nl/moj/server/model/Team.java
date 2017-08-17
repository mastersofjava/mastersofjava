package nl.moj.server.model;

import lombok.Data;

@Data
public class Team {
	private int id;
	private String name;
	private String password;
	private String cpassword;
	
}
