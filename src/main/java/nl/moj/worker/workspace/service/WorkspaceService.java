package nl.moj.worker.workspace.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class WorkspaceService {

    public Workspace getWorkspace(AssignmentDescriptor ad, Map<Path,String> sources) throws IOException {
        return new BasicWorkspace(ad,sources);
    }
}
