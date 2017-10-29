package nl.moj.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "moj.server.directories")
@Configuration
public class DirectoriesConfiguration {

	private String baseDirectory;
	private String teamDirectory;
	private String assignmentDirectory;
	private String libDirectory;

	public String getBaseDirectory() {
		return baseDirectory;
	}

	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	public String getTeamDirectory() {
		return teamDirectory;
	}

	public void setTeamDirectory(String teamDirectory) {
		this.teamDirectory = teamDirectory;
	}

	public String getAssignmentDirectory() {
		return assignmentDirectory;
	}

	public void setAssignmentDirectory(String assignmentDirectory) {
		this.assignmentDirectory = assignmentDirectory;
	}

	public String getLibDirectory() {
		return libDirectory;
	}

	public void setLibDirectory(String libDirectory) {
		this.libDirectory = libDirectory;
	}

}