package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentFiles {

	@JsonProperty("assignment")
	private Path assignment;
	@JsonProperty("editable")
	private List<Path> editable;
	@JsonProperty("readonly")
	private List<Path> readonly;
	@JsonProperty("tests")
	private List<Path> tests;
	@JsonProperty("hidden-tests")
	private List<Path> hiddenTests;
	@JsonProperty("solution")
	private List<Path> solution;
}
