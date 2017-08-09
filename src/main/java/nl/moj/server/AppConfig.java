package nl.moj.server;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.CacheControl;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import nl.moj.server.async.CompletableExecutors;
import nl.moj.server.async.TimedCompletables;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingStrategy;

@Configuration
@EnableAspectJAutoProxy
public class AppConfig {

	private static final String DIRECTORY = "./assignments";


	@Configuration
	public class CompilerConfig {
		@Bean
		public JavaCompiler systemJavaCompiler() {
			return ToolProvider.getSystemJavaCompiler();
		}

		@Bean
		@Scope(SCOPE_PROTOTYPE)
		public DiagnosticCollector<JavaFileObject> diagnosticCollector() {
			return new DiagnosticCollector<>();
		}

		@Bean
		public StandardJavaFileManager standardJavaFileManager(JavaCompiler javaCompiler,
				DiagnosticCollector<JavaFileObject> diagnosticCollector) {
			final Charset charset = StandardCharsets.UTF_8;

			return javaCompiler.getStandardFileManager(diagnosticCollector, null, charset);
		}
	}


	@EnableWebSecurity
	@Configuration
	public class SecurityConfig {

		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication().withUser("team1").password("team1").roles("USER").and().withUser("team2")
					.password("team2").roles("USER");
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

		@Override
		public void configureMessageBroker(MessageBrokerRegistry config) {
			config.enableSimpleBroker("/topic", "/queue"); // ,"/user"
			config.setApplicationDestinationPrefixes("/app");
			config.setUserDestinationPrefix("/user");
		}

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/submit").withSockJS();
		}
	}

	@Configuration
	public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages.simpDestMatchers("/*").authenticated();
		}

		@Override
		protected boolean sameOriginDisabled() {
			// disable CSRF for websockets for now...
			return true;
		}
	}

	@EnableWebMvc
	@Configuration
	public class WebAppConfig extends WebMvcConfigurerAdapter {

		private ApplicationContext applicationContext;

		@Autowired
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		public WebAppConfig() {

		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {

			registry.addResourceHandler("/webjars/**")
					.addResourceLocations("classpath:/META-INF/resources/webjars/", "/webjars/")
					.setCacheControl(CacheControl.maxAge(30L, TimeUnit.DAYS).cachePublic()).resourceChain(true)
					.addResolver(new WebJarsResourceResolver());
			registry.addResourceHandler("/**", "/static/**").addResourceLocations("classpath:/static/");
		}

		@Bean
		public ViewResolver javascriptViewResolver() {

			ThymeleafViewResolver resolver = new ThymeleafViewResolver();
			resolver.setTemplateEngine(templateEngine(javascriptTemplateResolver()));
			resolver.setContentType("application/javascript");
			resolver.setCharacterEncoding("UTF-8");
			String[] viewnames = { "*.js" };
			resolver.setViewNames(viewnames);
			return resolver;
		}

		private TemplateEngine templateEngine(ITemplateResolver templateResolver) {
			SpringTemplateEngine engine = new SpringTemplateEngine();
			engine.addDialect(new LayoutDialect(new GroupingStrategy()));
			// engine.addDialect(new Java8TimeDialect());
			engine.setTemplateResolver(templateResolver);
			// engine.setTemplateEngineMessageSource(messageSource());
			return engine;
		}

		private ITemplateResolver javascriptTemplateResolver() {
			SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
			resolver.setApplicationContext(applicationContext);
			// resolver.setPrefix("/js/");
			resolver.setCacheable(false);
			resolver.setTemplateMode(TemplateMode.JAVASCRIPT);
			return resolver;
		}
	}

	@Configuration
	public class IntegrationConfig {
		@Bean
		public IntegrationFlow processFileFlow() {
			return IntegrationFlows.from("fileInputChannel").transform(fileToStringTransformer())
					.handle("fileProcessor", "process").get();
		}

		@Bean
		public MessageChannel fileInputChannel() {
			return new DirectChannel();
		}

		@Bean
		@InboundChannelAdapter(value = "fileInputChannel", poller = @Poller(fixedDelay = "1000"))
		public MessageSource<File> fileReadingMessageSource() {
			CompositeFileListFilter<File> filters = new CompositeFileListFilter<>();
			filters.addFilter(new SimplePatternFileListFilter("*.java"));
			LastModifiedFileListFilter lastmodified = new LastModifiedFileListFilter();
			lastmodified.setAge(1, TimeUnit.SECONDS);
			filters.addFilter(lastmodified);
			filters.addFilter(new AcceptOnceFileListFilter<>());

			FileReadingMessageSource source = new FileReadingMessageSource();
			source.setUseWatchService(true);
			source.setAutoCreateDirectory(true);
			source.setDirectory(new File(DIRECTORY));
			source.setFilter(filters);
			return source;
		}

		@Bean
		public FileToStringTransformer fileToStringTransformer() {
			return new FileToStringTransformer();
		}

		@Bean
		public FileProcessor fileProcessor() {
			return new FileProcessor();
		}
	}

	@Configuration
	@EnableAsync
	public class SpringAsyncConfig implements AsyncConfigurer {
		protected final Log logger = LogFactory.getLog(getClass());

		@Override
		public Executor getAsyncExecutor() {
			ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("async-%d").build();
			return CompletableExecutors.completable(Executors.newFixedThreadPool(10, threadFactory));
		}

		@Bean(name = "timed")
		public Executor timeoutExecutor() {
			ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("timed-%d").build();
			return TimedCompletables.timed(Executors.newFixedThreadPool(10, threadFactory), Duration.ofSeconds(2));
		}

		@Override
		public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
			return (ex, method, params) -> logger.error("Uncaught async error", ex);
		}
	}

	@Bean
	public Properties properties() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(DIRECTORY + "/puzzle.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prop;
	}

}
