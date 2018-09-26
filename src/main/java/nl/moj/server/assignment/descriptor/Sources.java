package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sources {

	@JsonProperty("base")
	private Path base;

	@JsonProperty("editable")
	private List<Path> editable;

	@JsonProperty("readonly")
	private List<Path> readonly;

	public List<Path> getEditable() {
		if( editable == null ) {
			return Collections.emptyList();
		}
		return editable;
	}

	public List<Path> getReadonly() {
		if( readonly == null ) {
			return Collections.emptyList();
		}
		return readonly;
	}
}
