package nl.moj.server.sound;

public enum Sound {

	TIC_TAC("slowtictac.wav"),
	GONG("gong.wav");

	private String filename;

	private Sound(String filename) {
		this.filename = filename;
	}

	public String filename() {
		return filename;
	}
}
