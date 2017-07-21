package nl.moj.server;

public class SourceMessage {

	private String team;
	private String source;

	public SourceMessage() {
	}
	
	public SourceMessage(String team, String source) {
		this.team = team;
		this.source = source;
	}
	
	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

}
