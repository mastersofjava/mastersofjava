package nl.moj.server;

public class JavaFile {

	private final String name;

	private final String filename;

	private final String content;

	public JavaFile(String filename, String content) {
		super();
		this.name = filename.substring(0, filename.indexOf("."));
		this.filename = filename;
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public String getFilename() {
		return filename;
	}

	public String getContent() {
		return content;
	}

}
