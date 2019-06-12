package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentFiles {

	@JsonProperty("assignment")
	private Path assignment;
	
	@JsonProperty("solution")
	private List<Path> solution = new ArrayList<>();

	@JsonProperty("sources")
	private Sources sources = new Sources();

	@JsonProperty("resources")
	private Resources resources = new Resources();

	@JsonProperty("test-sources")
	private TestSources testSources = new TestSources();

	@JsonProperty("test-resources")
	private TestResources testResources = new TestResources();

	@JsonProperty("security-policy")
	private Optional<Path> securityPolicy = Optional.empty();
}
