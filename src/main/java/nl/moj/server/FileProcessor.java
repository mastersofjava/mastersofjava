package nl.moj.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;

import nl.moj.server.files.JavaFile;

public class FileProcessor {
    private static final String HEADER_FILE_NAME = "file_name";
    private static final String MSG = "%s received";
    
    @Autowired
    private AssignmentService assignmentService;
    
    
    public void process(Message<String> msg) {
        String fileName = (String) msg.getHeaders().get(HEADER_FILE_NAME);
        String content = msg.getPayload();

        System.out.println(String.format(MSG, fileName));
        JavaFile file = new JavaFile(fileName, content);
        assignmentService.addFile(file);
    }
}