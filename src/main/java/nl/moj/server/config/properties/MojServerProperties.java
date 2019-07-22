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
	private Limits limits = new Limits();
	private Directories directories = new Directories();
	private Languages languages = new Languages();
	private Runtimes runtimes = new Runtimes();
	private Competition competition;

}
