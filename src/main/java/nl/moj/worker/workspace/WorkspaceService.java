package nl.moj.worker.workspace;

import java.io.IOException;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.*;
import nl.moj.common.messages.JMSFile;
import nl.moj.worker.java.common.FileContent;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class WorkspaceService {

    public Workspace getWorkspace(AssignmentDescriptor ad, List<JMSFile> replacements) throws IOException {
        return new BasicWorkspace(ad,replacements);
    }
}
