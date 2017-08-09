package nl.moj.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;

import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;

public class FileProcessor {
	private static final String HEADER_FILE_NAME = "file_name";
	private static final String MSG = "%s received";

	@Autowired
	private AssignmentService assignmentService;

	public void process(Message<String> msg) {
		String filename = (String) msg.getHeaders().get(HEADER_FILE_NAME);
		String content = msg.getPayload();

		System.out.println(String.format(MSG, filename));
		String type = filename.substring(filename.indexOf("."));
		AssignmentFile file = null;
		switch (type) {
		case ".java":
			if (filename.toLowerCase().contains("test")) {
				file = new AssignmentFile(filename, content, FileType.TEST);
			} else {
				file = new AssignmentFile(filename, content, FileType.JAVA_SOURCE);
			}
			break;
		case ".txt":
			file = new AssignmentFile(filename, content, FileType.TASK);
			break;
		default:
			break;
			
		}
		assignmentService.addFile(file);
	}
}