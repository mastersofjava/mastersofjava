package nl.moj.server;

import nl.moj.server.config.properties.MojServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@EnableConfigurationProperties(MojServerProperties.class)
public class MojServerApplication {

    public static void main(String[] args) {
		SpringApplication.run(MojServerApplication.class, args);
	}

    @Bean
	public TaskScheduler taskScheduler() {
		ScheduledExecutorService localExecutor = Executors.newScheduledThreadPool(5);
		return new ConcurrentTaskScheduler(localExecutor);
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