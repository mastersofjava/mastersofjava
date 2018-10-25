package nl.moj.server.login;

import lombok.Data;

@Data
public class SignupForm {

	private String name;

	private String password;

	private String passwordCheck;

	private String country;

	private String company;

}
