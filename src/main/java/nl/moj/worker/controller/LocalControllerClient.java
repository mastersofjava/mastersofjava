package nl.moj.worker.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;

@RequiredArgsConstructor
@Slf4j
public class LocalControllerClient implements ControllerClient {

    private final AssignmentService assignmentService;

    public Path getAssigmentContentFolder(UUID assignmentUuid) {
        return assignmentService.getAssignmentContentFolder(assignmentUuid);
    }

    public AssignmentDescriptor getAssignmentDescriptor(UUID assignmentUuid) throws IOException {

//        Path assignment = getAssigmentContentFolder(assignmentUuid);
//        Path descriptor = assignment.resolve("assignment.yaml");
//        if (!Files.exists(descriptor)) {
//            descriptor = assignment.resolve("assignment.yml");
//        }
//        if (!Files.exists(descriptor)) {
//            throw new IOException("Unable to resolve assignment descriptor for assignment " + assignmentUuid + ".");
//        }
//
//        try {
//            AssignmentDescriptor ad = yamlMapper.readValue(Files.newInputStream(descriptor),
//                    AssignmentDescriptor.class);
//            ad.setDirectory(descriptor.getParent());
//            ad.setOriginalAssignmentDescriptor(descriptor.toString());
//            return ad;
//        } catch (Exception e) {
//            throw new IOException("Unable to read assignment descriptor " + descriptor + ".",
//                    e);
//        }

        return assignmentService.resolveAssignmentDescriptor(assignmentUuid);
    }
}
