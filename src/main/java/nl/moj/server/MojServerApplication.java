package nl.moj.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@EnableConfigurationProperties(MojServerProperties.class)
@RequiredArgsConstructor
@Slf4j
public class MojServerApplication {

	private final BootstrapService bootstrapService;

    public static void main(String[] args) {
		SpringApplication.run(MojServerApplication.class, args);
	}

    @Bean
	public TaskScheduler taskScheduler() {
		ScheduledExecutorService localExecutor = Executors.newScheduledThreadPool(5);
		return new ConcurrentTaskScheduler(localExecutor);
	}

	@PostConstruct
	public void bootstrap() throws IOException {
		bootstrapService.bootstrap();
	}
}