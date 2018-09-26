package nl.moj.server;

import lombok.RequiredArgsConstructor;
import nl.moj.server.config.properties.Directories;
import nl.moj.server.config.properties.MojServerProperties;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@EnableConfigurationProperties(MojServerProperties.class)
@RequiredArgsConstructor
public class MojServerApplication {

	private final MojServerProperties mojServerProperties;

    public static void main(String[] args) {
		SpringApplication.run(MojServerApplication.class, args);
	}

    @Bean
	public TaskScheduler taskScheduler() {
		ScheduledExecutorService localExecutor = Executors.newScheduledThreadPool(5);
		return new ConcurrentTaskScheduler(localExecutor);
	}

	@PostConstruct
	public void initialize() {
		Directories directories = mojServerProperties.getDirectories();

		if (!FileUtils.getFile(directories.getBaseDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory()).mkdirs();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory()).mkdirs();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory()).mkdirs();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getLibDirectory()).mkdirs();
		}
		if (!FileUtils.getFile(directories.getBaseDirectory(), directories.getSoundDirectory()).exists()) {
			FileUtils.getFile(directories.getBaseDirectory(), directories.getSoundDirectory()).mkdirs();
		}
	}
}