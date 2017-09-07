package nl.moj.server.files;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;

import nl.moj.server.AppConfig;
import nl.moj.server.competition.Competition;

public class FileProcessor {

	private static final Logger log = LoggerFactory.getLogger(FileProcessor.class);

	private static final String HEADER_FILE_NAME = "file_name";
	private static final String HEADER_FILE_ORIGINALFILE = "file_originalFile";

	@Autowired
	private Competition competition;

	public void process(Message<String> msg) {
		String filename = (String) msg.getHeaders().get(HEADER_FILE_NAME);
		File origFile = (File) msg.getHeaders().get(HEADER_FILE_ORIGINALFILE);
		String origFilename = origFile.getAbsolutePath();

		int beginIndex = origFilename.indexOf(AppConfig.DIRECTORY) + AppConfig.DIRECTORY.length() + 1;
		int indexOf = origFilename.indexOf("/", beginIndex);
		String assignment = origFilename.substring(beginIndex, indexOf);
		log.info("assignment: {}", assignment);

		log.info("{} received", filename);
		String type = filename.substring(filename.indexOf("."));
		AssignmentFile file = null;
		String content = msg.getPayload();
		switch (type) {
		case ".java":
			if (competition.getAssignment(assignment).getEditableFileNames().contains(filename)) {
				file = new AssignmentFile(filename, content, FileType.EDIT, assignment, origFile);
			} else if (competition.getAssignment(assignment).getTestFileNames().contains(filename)) {
				file = new AssignmentFile(filename, content, FileType.TEST, assignment, origFile);
			} else if (competition.getAssignment(assignment).getSubmitFileNames().contains(filename)) {
				file = new AssignmentFile(filename, content, FileType.SUBMIT, assignment, origFile);
			} else if (competition.getAssignment(assignment).getSolutionFileNames().contains(filename)) {
				file = new AssignmentFile(filename, content, FileType.SOLUTION, assignment, origFile);
			} else {
				file = new AssignmentFile(filename, content, FileType.READONLY, assignment, origFile);
			}
			competition.addAssignmentFile(file);
			break;
		case ".txt":
			file = new AssignmentFile(filename, content, FileType.TASK, assignment, origFile);
			competition.addAssignmentFile(file);
			break;
		case ".xml":
			if (filename.equalsIgnoreCase("pom.xml")) {
				file = new AssignmentFile(filename, content, FileType.POM, assignment, origFile);
				competition.addAssignmentFile(file);
				competition.setCurrentAssignment(assignment);
			}
		default:
			break;
		}
	}

}