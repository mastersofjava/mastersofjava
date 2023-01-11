package nl.moj.server.runtime.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.assignment.service.AssignmentServiceException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuntimeService {

    private final AssignmentService assignmentService;

    private final AssignmentRepository assignmentRepository;

    private final YAMLMapper yamlMapper;

    public Path getAssigmentContentFolder(UUID assignmentUuid) throws IOException {
        Assignment assignment = assignmentRepository.findByUuid(assignmentUuid);
        return assignmentService.getAssignmentContentFolder(assignment);
    }

    public AssignmentDescriptor getAssignmentDescriptor(UUID assignmentUuid) throws IOException {

        Path assignment = getAssigmentContentFolder(assignmentUuid);
        Path descriptor = assignment.resolve("assignment.yaml");
        if( !Files.exists(descriptor)) {
            descriptor = assignment.resolve("assignment.yml");
        }
        if( !Files.exists(descriptor)) {
            throw new IOException("Unable to resolve assignment descriptor for assignment " + assignmentUuid + ".");
        }

        try {
            AssignmentDescriptor ad = yamlMapper.readValue(Files.newInputStream(descriptor),
                    AssignmentDescriptor.class);
            ad.setDirectory(descriptor.getParent());
            ad.setOriginalAssignmentDescriptor(descriptor.toString());
            return ad;
        } catch (Exception e) {
            throw new AssignmentServiceException("Unable to read assignment descriptor " + descriptor + ".",
                    e);
        }
    }
}
