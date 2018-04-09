package nl.moj.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DirectoriesConfiguration.class)
public class MojServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MojServerApplication.class, args);
	}

//	@PostConstruct
//	public void initialize() {
//		if (!FileUtils.getFile(directories.getBaseDirectory()).exists()) {
//			FileUtils.getFile(directories.getBaseDirectory()).mkdir();
//		}
//		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).exists()) {
//			FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).mkdir();
//		}
//		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).exists()) {
//			FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).mkdir();
//		}
//		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).exists()) {
//			FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).mkdir();
//		}
//		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getSoundDirectory()).exists()) {
//			FileUtils.getFile(directories.getBaseDirectory(), directories.getSoundDirectory()).mkdir();
//		}
//	}

}