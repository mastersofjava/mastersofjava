package nl.moj.server.files;

public class AssignmentFile {

	private final String name;

	private final String filename;

	private final String content;

	private final FileType fileType;
	
	public AssignmentFile(String filename, String content, FileType fileType) {
		super();
		this.name = filename.substring(0, filename.indexOf("."));
		this.filename = filename;
		this.content = content;
		this.fileType = fileType;
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

	public FileType getFileType() {
		return fileType;
	}

}
