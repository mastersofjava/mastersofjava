package nl.moj.server.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.util.NamedThreadFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
@AllArgsConstructor
public class AsyncConfiguration implements AsyncConfigurer {

	private MojServerProperties mojServerProperties;

	@Bean(name = "parallel")
	public ExecutorService parallelExecutor() {
		return Executors.newFixedThreadPool(mojServerProperties.getRuntime().getGameThreads(), new NamedThreadFactory("parallel"));
	}
	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> log.error("Uncaught async error", ex);
	}

	@Bean(name = "sequential")
	public ExecutorService sequentialExecutor() {
		return Executors.newFixedThreadPool(1, new NamedThreadFactory("sequential"));
	}
}
