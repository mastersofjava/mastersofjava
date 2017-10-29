package nl.moj.server;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	
	private DirectoriesConfiguration directories;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	public Application(DirectoriesConfiguration directories) {
		super();
		this.directories = directories;
	}

	@PostConstruct
	public void init() {
		if (!FileUtils.getFile(directories.getBaseDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory()).mkdir();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).mkdir();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).mkdir();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).mkdir();
		}
	}

}