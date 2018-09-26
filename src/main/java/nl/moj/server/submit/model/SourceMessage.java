package nl.moj.server.submit.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import nl.moj.server.submit.json.SourceMessageDeserializer;

import java.util.List;
import java.util.Map;

@JsonDeserialize(using = SourceMessageDeserializer.class)
@Data
public class SourceMessage {

	private Map<String, String> source;
	private List<String> tests;

	public SourceMessage(Map<String, String> source, List<String> tests) {
		this.source = source;
		this.tests = tests;
	}
}
