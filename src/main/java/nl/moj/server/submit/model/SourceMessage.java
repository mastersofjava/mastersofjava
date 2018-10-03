package nl.moj.server.submit.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.moj.server.submit.json.SourceMessageDeserializer;

import java.util.List;
import java.util.Map;

@JsonDeserialize(using = SourceMessageDeserializer.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourceMessage {

	private Map<String, String> sources;
	private List<String> tests;
}