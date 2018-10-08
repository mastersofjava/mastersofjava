package nl.moj.server.config.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Languages {

	@NotEmpty
	private List<JavaVersion> javaVersions = new ArrayList<>();

	public JavaVersion getJavaVersion(Integer version) {
		
		log.debug("Configured versions: ");
		javaVersions.forEach( jv -> {
			log.debug("Version " + jv.getVersion());
			log.debug("  Compiler " + jv.getCompiler());
			log.debug("  Runtime " + jv.getRuntime());
			log.debug("  -> available =  " + isAvailable(jv));
			log.debug("  -> version ok =  " + (jv.getVersion() >= version));
			log.debug("  -> version runtime = " + JavaVersionUtil.getRuntimeMajorVersion(jv));
		});
		
		return javaVersions.stream()
				.filter( this::isAvailable)
				.filter( jv -> jv.getVersion() >= version)
				.findFirst()
				.orElseGet( () -> javaHomeFallback(version));
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
				log.debug("Using JAVA_HOME since it if an appriopriate version");
			} else {
				log.warn("Unable to find runtime for Java version " + version + ", using the fallback version as found in JAVA_HOME which may be an incorrect version");
			}
			return v;
		} else {
			throw new IllegalArgumentException("No java version defined and no JAVA_HOME specified, cannot run without a javac/java...");
		}
	}

	@Data
	public static class JavaVersion {

		private Integer version;
		private String name;
		private Path compiler;
		private Path runtime;

	}

}
