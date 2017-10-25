package nl.moj.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.thymeleaf.spring5.ISpringTemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import nl.moj.server.files.AssignmentFileFilter;
import nl.moj.server.files.FileProcessor;
//import nz.net.ultraq.thymeleaf.LayoutDialect;
//import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingStrategy;

@Configuration
// @EnableAspectJAutoProxy
// @Import(AppConfig.SecurityConfig.class)
public class AppConfig {

	@Value("${moj.server.assignmentDirectory}")
	public String assignmentDirectory;

	@Value("${moj.server.threads}")
	private int threads;

	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

	@Configuration
	public class CompilerConfig {
		@Bean
		public JavaCompiler systemJavaCompiler() {
			return ToolProvider.getSystemJavaCompiler();
		}

		@Bean
		public DiagnosticCollector<JavaFileObject> diagnosticCollector() {
			return new DiagnosticCollector<>();
		}

		@Bean
		public StandardJavaFileManager standardJavaFileManager(JavaCompiler javaCompiler,
				DiagnosticCollector<JavaFileObject> diagnosticCollector) {
			return javaCompiler.getStandardFileManager(diagnosticCollector, null, StandardCharsets.UTF_8);
		}
	}

    @Bean
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(100000);
        container.setMaxBinaryMessageBufferSize(100000);
        return container;
    }

    @EnableWebSecurity
    @Configuration
    public class SecurityConfig extends WebSecurityConfigurerAdapter {
		@Autowired
		TeamDetailsService teamDetailsService = new TeamDetailsService();

		@Bean
		public PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}

		@Bean
		public DaoAuthenticationProvider authProvider() {
			DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
			authProvider.setUserDetailsService(teamDetailsService);
			authProvider.setPasswordEncoder(passwordEncoder());
			return authProvider;
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.authenticationProvider(authProvider());
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests() //
			.antMatchers("/login", "/register", "/feedback").permitAll()//
			.antMatchers("/").hasRole("USER") //
			.antMatchers("/control").hasRole("CONTROL") //
			
			.and().formLogin()
					.successHandler(new CustomAuthenticationSuccessHandler()).loginPage("/login").and().logout().and()
					.headers().frameOptions().disable().and().csrf().disable();
		}

		public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

			@Override
			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
					Authentication authentication) throws IOException, ServletException {
				// set our response to OK status
				response.setStatus(HttpServletResponse.SC_OK);
				boolean admin = false;

				for (GrantedAuthority auth : authentication.getAuthorities()) {
					if ("ROLE_CONTROL".equals(auth.getAuthority())) {
						admin = true;
					}
				}

				if (admin) {
					response.sendRedirect("/control");
				} else {
					response.sendRedirect("/");
					Path teamdir = Paths.get(basedir, teamDirectory, authentication.getName());
					if (!Files.exists(teamdir)) {
						try {
							Files.createDirectories(teamdir);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
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
			registry.addEndpoint("/control").withSockJS();
			registry.addEndpoint("/rankings").withSockJS();
			registry.addEndpoint("/feedback").withSockJS();
		}
	}

	// @Configuration
	// public class WebSocketSecurityConfig extends
	// AbstractSecurityWebSocketMessageBrokerConfigurer {
	//
	// protected void configureInbound(MessageSecurityMetadataSourceRegistry
	// messages) {
	// messages.simpDestMatchers("/*").authenticated();
	// }
	//
	// @Override
	// protected boolean sameOriginDisabled() {
	// // disable CSRF for websockets for now...
	// return true;
	// }
	// }

	@EnableWebMvc
	@Configuration
	public class WebAppConfig implements WebMvcConfigurer {

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
			String[] viewnames = { "/js/*.js" };
			resolver.setViewNames(viewnames);
			return resolver;
		}

		private ISpringTemplateEngine templateEngine(ITemplateResolver templateResolver) {
			SpringTemplateEngine engine = new SpringTemplateEngine();
			// engine.addDialect(new LayoutDialect(new GroupingStrategy()));
			// engine.addDialect(new Java8TimeDialect());
			engine.setTemplateResolver(templateResolver);
			// engine.setTemplateEngineMessageSource(messageSource());
			return engine;
		}

		private ITemplateResolver javascriptTemplateResolver() {
			SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
			resolver.setApplicationContext(applicationContext);
			resolver.setPrefix("/js/");
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
		public Comparator<File> comparator() {
			// make sure pom.xml is read first
			return new Comparator<File>() {

				@Override
				public int compare(File o1, File o2) {
					if (o1.getName().equalsIgnoreCase("pom.xml"))
						return -10;
					return 10;
				}

			};
		}

		@Bean
		@InboundChannelAdapter(value = "fileInputChannel", poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "1000"))
		public MessageSource<File> fileReadingMessageSource() {
			CompositeFileListFilter<File> filters = new CompositeFileListFilter<>();
			filters.addFilter(new IgnoreHiddenFileListFilter());
			filters.addFilter(new AssignmentFileFilter());
			FileReadingMessageSource source = new FileReadingMessageSource(comparator());
			source.setUseWatchService(true);
			source.setAutoCreateDirectory(true);
			source.setDirectory(new File(basedir, assignmentDirectory));
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
	public class AsyncConfig implements AsyncConfigurer {

		protected final Log logger = LogFactory.getLog(getClass());

		@Override
		public Executor getAsyncExecutor() {
			ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("async-%d").build();
			return Executors.newFixedThreadPool(threads, threadFactory);
		}

		@Bean(name = "compiling")
		public Executor compilingExecutor() {
			ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("compiling-%d").build();
			return Executors.newFixedThreadPool(threads, threadFactory);
		}

		@Bean(name = "testing")
		public Executor testingExecutor() {
			ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("testing-%d").build();

			return Executors.newFixedThreadPool(threads, threadFactory);
		}

		@Override
		public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
			return (ex, method, params) -> logger.error("Uncaught async error", ex);
		}

	}

	// class SimpleThreadFactory implements ThreadFactory {
	// public Thread newThread(Runnable r) {
	//
	// return new Thread(r);
	// }
	// }
	public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

		public SecurityWebApplicationInitializer() {
			super(AppConfig.SecurityConfig.class);
		}
	}
}