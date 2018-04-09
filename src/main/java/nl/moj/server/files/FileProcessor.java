package nl.moj.server.files;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.Competition;
import nl.moj.server.repository.TeamRepository;
import nl.moj.server.model.Test;
import nl.moj.server.repository.TestRepository;

@Component
@RequiredArgsConstructor
public class FileProcessor {

	private static final Logger log = LoggerFactory.getLogger(FileProcessor.class);
	private static final String HEADER_FILE_NAME = "file_name";
	private static final String HEADER_FILE_ORIGINALFILE = "file_originalFile";

	private final Competition competition;

	private final TestRepository testRepository;

	private final TeamRepository teamRepository;

    @Value("${moj.server.directories.assignmentDirectory}")
    private String DIRECTORY;

	public void process(Message<String> msg) {
		String filename = (String) msg.getHeaders().get(HEADER_FILE_NAME);
		File origFile = (File) msg.getHeaders().get(HEADER_FILE_ORIGINALFILE);
		if (origFile == null) {
			return;
		}
		String origFilename = origFile.getAbsolutePath();
		if (!origFilename.contains(DIRECTORY)) {
			return;
		}
		int beginIndex = origFilename.indexOf(DIRECTORY) + DIRECTORY.length() + 1;
		int indexOf = origFilename.indexOf(System.getProperty("file.separator"), beginIndex);
		String assignment = origFilename.substring(beginIndex, indexOf);

		log.info("{} received for assignment {}", filename, assignment);
		String type = filename.substring(filename.indexOf("."));
		AssignmentFile file = null;
		String content = msg.getPayload();
		switch (type) {
			case ".java":
				if (competition.getAssignment(assignment).getEditableFileNames().contains(filename)) {
					file = new AssignmentFile(filename, content, FileType.EDIT, assignment, origFile);
				} else if (competition.getAssignment(assignment).getTestFileNames().contains(filename)) {
					file = new AssignmentFile(filename, content, FileType.TEST, assignment, origFile);
					teamRepository.findAllByRole("ROLE_USER").forEach(team -> testRepository.save(new Test(team, assignment,
                            filename.substring(0, filename.indexOf(".")), 0, 0)));
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
				}
			default:
				break;
		}
	}

}