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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@Slf4j
@AllArgsConstructor
public class AsyncConfiguration implements AsyncConfigurer {

	private MojServerProperties mojServerProperties;

	@Override
	public Executor getAsyncExecutor() {
		return null;
	}

	@Bean(name = "compiling")
	public Executor compilingExecutor() {
		return Executors.newFixedThreadPool(mojServerProperties.getRuntimes().getCompile().getThreads(), new NamedThreadFactory("compiling"));
	}

	@Bean(name = "testing")
	public Executor testingExecutor() {
		return Executors.newFixedThreadPool(mojServerProperties.getRuntimes().getTest().getThreads(), new NamedThreadFactory("testing"));
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> log.error("Uncaught async error", ex);
	}

}
