package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;

@Data
@ConfigurationProperties(prefix="moj.server.directories")
public class Directories {

	@NotNull
	private String baseDirectory;
	private String teamDirectory = "teams";
	private String assignmentDirectory = "assignments";
	private String libDirectory = "lib";
	private String soundDirectory = "sounds";
	private String javadocDirectory = "javadoc";
	private String resourceDirectory = "resources";
	
}