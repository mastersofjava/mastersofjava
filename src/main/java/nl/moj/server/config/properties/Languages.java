package nl.moj.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotEmpty;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "moj.server.languages")
public class Languages {

	@NotEmpty
	private List<JavaVersion> javaVersions = new ArrayList<>();

	public JavaVersion getJavaVersion(String version) {
		return javaVersions.stream()
				.filter(v -> v.getVersion().equals(version))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Cannot locate runtime for java version " + version));
	}

	// TODO fix this shortcut in a propper way using assignment language setting, ignored for now.
	public JavaVersion getJavaVersion() {
		if( javaVersions.size() == 1 ) {
			return javaVersions.get(0);
		}
		throw new IllegalArgumentException("Multiple java versions found, specify the version you want.");
	}

	@Data
	public static class JavaVersion {

		private String version;
		private String name;
		private Path compiler;
		private Path runtime;

	}

}
