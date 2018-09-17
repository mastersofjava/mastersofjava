package nl.moj.server.config;

import lombok.AllArgsConstructor;
import nl.moj.server.config.properties.MojServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableScheduling
@AllArgsConstructor
public class TaskSchedulingConfiguration {

	private MojServerProperties mojServerProperties;

	@Bean
	public ScheduledExecutorFactoryBean scheduledExecutorFactoryBean() {
		ScheduledExecutorFactoryBean fb = new ScheduledExecutorFactoryBean();
		fb.setPoolSize(mojServerProperties.getRuntimes().getServer().getThreads());
		fb.setContinueScheduledExecutionAfterException(true);
		return fb;
	}

	@Bean
	public TaskScheduler taskScheduler(ScheduledExecutorService scheduledExecutorService) {
		return new ConcurrentTaskScheduler(scheduledExecutorService);
	}
}
