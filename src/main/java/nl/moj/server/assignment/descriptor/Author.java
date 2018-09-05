package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Author {

	@JsonProperty("name")
	private String name;
	@JsonProperty("company")
	private String company;
	@JsonProperty("website")
	private String website;
	
}
