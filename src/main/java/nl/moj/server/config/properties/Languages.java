package nl.moj.server.config.properties;

import lombok.Data;
import nl.moj.server.util.JavaVersionUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotEmpty;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "moj.server.languages")
public class Languages {

	@NotEmpty
	private List<JavaVersion> javaVersions = new ArrayList<>();

	public JavaVersion getJavaVersion(Integer version) {
		return javaVersions.stream()
				.filter(this::isAvailable)
				.filter( jv -> jv.getVersion() >= version)
				.findFirst()
				.orElse(javaHomeFallback(version));
	}
	
	private boolean isAvailable(JavaVersion javaVersion ) {
		return javaVersion.getCompiler().toFile().exists() &&
				javaVersion.getRuntime().toFile().exists() &&
				javaVersion.getVersion().equals(JavaVersionUtil.getRuntimeMajorVersion(javaVersion));
	}

	private JavaVersion javaHomeFallback(Integer version) {
		// we should still check if the specified version is available
		// on the fallback as source and target version
		String javaHome = System.getenv("JAVA_HOME");
		if(StringUtils.isNotBlank(javaHome)) {
			JavaVersion v = new JavaVersion();
			v.setCompiler(Paths.get(javaHome, "bin", "javac"));
			v.setRuntime(Paths.get(javaHome,"bin","java"));
			v.setName("fallback");
			v.setVersion(JavaVersionUtil.getRuntimeMajorVersion(v));

			if( version != null && version <= v.getVersion()) {
				return v;
			}
		}
		throw new IllegalArgumentException("Unable to find runtime for Java version " + version);
	}

	@Data
	public static class JavaVersion {

		private Integer version;
		private String name;
		private Path compiler;
		private Path runtime;

	}

}
