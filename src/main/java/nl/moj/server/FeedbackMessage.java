package nl.moj.server;

public class FeedbackMessage {

	private String team;
	private String text;
	private String time;

	public FeedbackMessage(String team, String text, String time) {
		super();
		this.team = team;
		this.text = text;
		this.time = time;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}



}
