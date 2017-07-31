package nl.moj.server;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.PathResourceResolver;
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

import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingStrategy;

@Configuration
public class AppConfig {

	@EnableWebSecurity
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

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {

			registry.addResourceHandler("/webjars/**")
					.addResourceLocations("classpath:/META-INF/resources/webjars/", "/webjars/")
					.setCacheControl(CacheControl.maxAge(30L, TimeUnit.DAYS).cachePublic()).resourceChain(true)
					.addResolver(new WebJarsResourceResolver());
			registry.addResourceHandler("/**","/static/**")
					.addResourceLocations("classpath:/static/");
					//.resourceChain(true)
					//.addResolver(new PathResourceResolver());
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

}
