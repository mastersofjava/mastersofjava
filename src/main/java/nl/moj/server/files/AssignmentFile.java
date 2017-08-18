package nl.moj.server.files;

public class AssignmentFile {

	private final String name;

	private final String filename;

	private final String content;

	private final FileType fileType;

	private final String assignment;

	public AssignmentFile(String filename, String content, FileType fileType, String assignment) {
		super();
		this.name = filename.substring(0, filename.indexOf("."));
		this.filename = filename;
		this.content = content;
		this.fileType = fileType;
		this.assignment = assignment;
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

	public String getAssignment() {
		return assignment;
	}

}
