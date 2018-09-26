package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Resources {

	@JsonProperty("base")
	private Path base;

	@JsonProperty("files")
	private List<Path> files;

	public List<Path> getFiles() {
		if( files == null ) {
			return Collections.emptyList();
		}
		return files;
	}
}
