package nl.moj.server;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SourceMessageDeserializer.class)
public class SourceMessage {

	private String team;
	private Map<String, String> source;

	public SourceMessage() {
	}

	public SourceMessage(String team, Map<String, String> source) {
		this.team = team;
		this.source = source;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public Map<String, String> getSource() {
		return source;
	}

	public void setSource(Map<String, String> source) {
		this.source = source;
	}

}
