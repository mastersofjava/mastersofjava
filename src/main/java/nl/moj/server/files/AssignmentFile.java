package nl.moj.server.files;

import java.io.File;

public class AssignmentFile {

	private final String name;

	private final String filename;

	private final String content;

	private final FileType fileType;

	private final String assignment;
	
	private final File file;
	
	private boolean readOnly;

	public AssignmentFile(String filename, String content, FileType fileType, String assignment, File file) {
		super();
		this.name = filename.substring(0, filename.indexOf("."));
		this.filename = filename;
		this.content = content;
		this.fileType = fileType;
		this.assignment = assignment;
		this.file = file;
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

	public File getFile() {
		return file;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public AssignmentFile setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

}
