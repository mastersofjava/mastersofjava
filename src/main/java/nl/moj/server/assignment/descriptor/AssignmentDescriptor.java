package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentDescriptor {

	private Path directory;

	@JsonProperty("name")
	private String name;
	@JsonProperty("display-name")
	private String displayName;
	@JsonProperty("author")
	private Author author;
	@JsonProperty("image")
	private Path image;
	@JsonProperty("sponsor-image")
	private Path sponsorImage;
	@JsonProperty("labels")
	private List<String> labels;
	@JsonProperty("difficulty")
	private Integer difficulty;
	@JsonProperty("java-version")
	private Integer javaVersion;
	@JsonProperty("duration")
	private Duration duration;
	@JsonProperty("submit-timeout")
	private Duration submitTimeout;
	@JsonProperty("test-timeout")
	private Duration testTimeout;
	@JsonProperty("compile-timeout")
	private Duration compileTimeout;
	@JsonProperty("execution-model")
	private ExecutionModel executionModel;
	
	@JsonProperty("scoring-rules")
	private ScoringRules scoringRules;
	@JsonProperty("assignment-files")
	private AssignmentFiles assignmentFiles;
}
