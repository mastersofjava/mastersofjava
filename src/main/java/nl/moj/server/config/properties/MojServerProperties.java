package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix="moj.server")
@Data
public class MojServerProperties {

	private Path assignmentRepo;
	private Limits limits;
	private Directories directories;
	private Languages languages;
	private Runtimes runtimes;
	private Competition competition;

}
