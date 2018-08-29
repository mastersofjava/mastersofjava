package nl.moj.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.moj.server.util.NamedThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class AppConfig {

	private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

	private int threads;

	private DirectoriesConfiguration directories;

	public AppConfig(@Value("${moj.server.threads}") int threads, DirectoriesConfiguration directories) {
		super();
		this.threads = threads;
		this.directories = directories;
	}

	@Bean
	public ScheduledExecutorFactoryBean scheduledExecutorFactoryBean() {
		return new ScheduledExecutorFactoryBean();
	}

	@Bean(name = "objectMapper")
	public ObjectMapper jsonObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper;
	}

	@Bean(name = "yamlObjectMapper")
	public ObjectMapper yamlObjectMapper() {
		ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
		yamlObjectMapper.registerModule(new JavaTimeModule());

		return yamlObjectMapper;
	}

//	@Bean
//	public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
//		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
//		container.setMaxTextMessageBufferSize(100000);
//		container.setMaxBinaryMessageBufferSize(100000);
//		return container;
//	}

	@Configuration
	public class WebConfig implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			Path path = Paths.get(directories.getJavaDocDirectory());
			if (!path.isAbsolute()) {
				path = Paths.get(directories.getBaseDirectory(), directories.getJavaDocDirectory());
				System.out.println("javadoc -> " + path.toAbsolutePath().toUri().toString());
			}
			registry
					.addResourceHandler("/javadoc/**")
					.addResourceLocations(path.toAbsolutePath().toUri().toString());
		}


	}

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

	@EnableWebSecurity
	@Configuration
	public class SecurityConfig extends WebSecurityConfigurerAdapter {

		private TeamDetailsService teamDetailsService;

		public SecurityConfig(TeamDetailsService teamDetailsService) {
			this.teamDetailsService = teamDetailsService;
		}

		@Bean
		public PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}

		private DaoAuthenticationProvider authProvider() {
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

					.and().formLogin().successHandler(new CustomAuthenticationSuccessHandler()).loginPage("/login")
					.and().logout().and().headers().frameOptions().disable().and().csrf().disable();
		}

		private class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

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
					Path teamdir = Paths.get(directories.getBaseDirectory(), directories.getTeamDirectory(), authentication.getName());
					if (!Files.exists(teamdir)) {
						try {
							Files.createDirectories(teamdir);
						} catch (IOException e) {
							log.error("error while creating team directory", e);
						}
					}
				}
			}
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

		@Override
		public void configureMessageBroker(MessageBrokerRegistry config) {
			config.enableSimpleBroker("/topic", "/queue");
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

	@Configuration
	@EnableAsync
	public class AsyncConfig implements AsyncConfigurer {

		protected final Log logger = LogFactory.getLog(getClass());

		@Override
		public Executor getAsyncExecutor() {
			return Executors.newFixedThreadPool(threads, new NamedThreadFactory("async"));
		}

		@Bean(name = "compiling")
		public Executor compilingExecutor() {
			return Executors.newFixedThreadPool(threads, new NamedThreadFactory("compiling"));
		}

		@Bean(name = "testing")
		public Executor testingExecutor() {
			return Executors.newFixedThreadPool(threads, new NamedThreadFactory("testing"));
		}

		@Override
		public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
			return (ex, method, params) -> logger.error("Uncaught async error", ex);
		}

	}

	public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

		public SecurityWebApplicationInitializer() {
			super(AppConfig.SecurityConfig.class);
		}
	}
}