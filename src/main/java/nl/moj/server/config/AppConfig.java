package nl.moj.server.config;

import com.fasterxml.jackson.databind.MapperFeature;
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
		objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
		return objectMapper;
	}

	@Bean(name = "yamlObjectMapper")
	public ObjectMapper yamlObjectMapper() {
		ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
		yamlObjectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
		yamlObjectMapper.registerModule(new JavaTimeModule());
		yamlObjectMapper.registerModule(new Jdk8Module());

		return yamlObjectMapper;
	}
}