package nl.moj.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;

import nl.moj.server.competition.Competition;
import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;

public class FileProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(FileProcessor.class);

	private static final String HEADER_FILE_NAME = "file_name";
	private static final String HEADER_FILE_ORIGINALFILE = "file_originalFile";

	@Autowired
	private AssignmentService assignmentService;
	
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
		switch (type) {
		case ".java":
			if (filename.toLowerCase().contains("test")) {
				file = new AssignmentFile(filename, msg.getPayload(), FileType.TEST, assignment, origFile);
			} else {
				file = new AssignmentFile(filename, msg.getPayload(), FileType.JAVA_SOURCE, assignment, origFile);
			}
			break;
		case ".txt":
			file = new AssignmentFile(filename, msg.getPayload(), FileType.TASK, assignment, origFile);
			break;
		case ".xml":
			if (filename.equalsIgnoreCase("pom.xml")) {
				file = new AssignmentFile(filename, msg.getPayload(), FileType.POM, assignment, origFile);
			}
		default:
			break;
			
		}
		if (file != null) {
			competition.addAssignmentFile(file);			
		}
	}
	

	
}