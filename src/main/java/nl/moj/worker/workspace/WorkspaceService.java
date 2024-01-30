package nl.moj.worker.workspace;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.messages.JMSFile;

@Service
@Slf4j
@AllArgsConstructor
public class WorkspaceService {

    public Workspace getWorkspace(AssignmentDescriptor ad, List<JMSFile> replacements) throws IOException {
        return new BasicWorkspace(ad, replacements);
    }
}
