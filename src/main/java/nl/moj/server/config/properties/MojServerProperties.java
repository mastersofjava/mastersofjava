package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@Data
@ConfigurationProperties(prefix="moj.server")
public class MojServerProperties {

	@NotNull
	private Path assignmentRepo;
	private Limits limits;
	private Directories directories;
	private Languages languages;
	private Runtimes runtimes;
	private Competition competition;

}
