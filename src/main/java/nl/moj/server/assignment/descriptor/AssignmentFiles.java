package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentFiles {

	@JsonProperty("assignment")
	private Path assignment;
	@JsonProperty("editable")
	private List<Path> editable = new ArrayList<>();
	@JsonProperty("readonly")
	private List<Path> readonly  = new ArrayList<>();
	@JsonProperty("tests")
	private List<Path> tests  = new ArrayList<>();
	@JsonProperty("hidden-tests")
	private List<Path> hiddenTests = new ArrayList<>();
	@JsonProperty("solution")
	private List<Path> solution = new ArrayList<>();
	
}
