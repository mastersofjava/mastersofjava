package nl.moj.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.tools.*;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableScheduling
public class AppConfig {

	@Bean(name = "objectMapper")
	public ObjectMapper jsonObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper;
	}

	@Bean(name = "yamlObjectMapper")
	public ObjectMapper yamlObjectMapper() {
		ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
		yamlObjectMapper.registerModule(new JavaTimeModule());
		yamlObjectMapper.registerModule(new Jdk8Module());

		return yamlObjectMapper;
	}

	//TODO this should be removed an use the language configuration.
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

//	public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {
//
//		public SecurityWebApplicationInitializer() {
//			super(AppConfig.SecurityConfig.class);
//		}
//	}

}