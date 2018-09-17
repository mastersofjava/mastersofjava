package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix="moj.server.directories")
public class Directories {

	private String baseDirectory;
	private String teamDirectory = "teams";
	private String assignmentDirectory = "assignments";
	private String libDirectory = "lib";
	private String soundDirectory = "sounds";
	private String javadocDirectory = "javadoc";
	
}