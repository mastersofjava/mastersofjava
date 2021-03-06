/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.assignment.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.model.AssignmentDescriptorValidationResult;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.runtime.JavaAssignmentFileResolver;
import nl.moj.server.runtime.model.AssignmentFile;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssignmentService {

    //FIXME this is needed for now to make sure we get the same uuids as they are not persisted atm.
    private static final Map<UUID, List<AssignmentFile>> ASSIGNMENT_FILES = new ConcurrentHashMap<>();

    private final ObjectMapper yamlObjectMapper;

    private final AssignmentRepository assignmentRepository;

    private final AssignmentDescriptorValidator assignmentDescriptorValidator;

    public AssignmentService(@Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper, AssignmentRepository assignmentRepository, AssignmentDescriptorValidator assignmentDescriptorValidator) {
        this.yamlObjectMapper = yamlObjectMapper;
        this.assignmentRepository = assignmentRepository;
        this.assignmentDescriptorValidator = assignmentDescriptorValidator;
    }

    public AssignmentDescriptor getAssignmentDescriptor(Assignment assignment) {
        try {
            Path descriptor = Paths.get(assignment.getAssignmentDescriptor());
            AssignmentDescriptor assignmentDescriptor = yamlObjectMapper.readValue(Files.newInputStream(descriptor), AssignmentDescriptor.class);
            assignmentDescriptor.setDirectory(descriptor.getParent());
            return assignmentDescriptor;
        } catch (Exception e) {
            throw new AssignmentServiceException("Unable to read assignment descriptor " + assignment.getAssignmentDescriptor() + ".", e);
        }
    }

    public List<Assignment> updateAssignments(Path base) throws AssignmentServiceException {
        log.info("Discovering assignments from {}.", base);
        List<Assignment> assignments = findAssignments(base);
        // validate
        List<String> invalid = assignments.stream()
                .map(this::validateAssignment)
                .filter(r -> !r.isValid())
                .map(AssignmentDescriptorValidationResult::getAssignment)
                .collect(Collectors.toList());
        if (invalid.isEmpty()) {
            // update or create
            return assignments.stream().map(d -> {
                Assignment current = assignmentRepository.findByName(d.getName());
                if (current != null) {
                    log.info("Updating existing assignment {}.", current.getName());
                    current.setAssignmentDescriptor(d.getAssignmentDescriptor());
                    return assignmentRepository.save(current);
                } else {
                    log.info("Added new assignment {}.", d.getName());
                    Assignment a = new Assignment();
                    a.setAssignmentDescriptor(d.getAssignmentDescriptor());
                    a.setName(d.getName());
                    a.setUuid(UUID.randomUUID());
                    return assignmentRepository.save(a);
                }
            }).collect(Collectors.toList());
        } else {
            throw new AssignmentServiceException("Problems during assignment update of assignment(s) '" + Strings.join(invalid, ',') +
                    "' see the logs for information. Correct problems and try again.");
        }
    }

    private AssignmentDescriptorValidationResult validateAssignment(Assignment a) {
        // check if assignment descriptor can be read.
        try {
            AssignmentDescriptor ad = getAssignmentDescriptor(a);
            AssignmentDescriptorValidationResult validationResult = assignmentDescriptorValidator.validate(ad);
            if (!validationResult.isValid()) {
                log.error("Validation of assignment {} failed. Problems found: \n{}", a.getName(), Strings.join(validationResult
                        .getValidationMessages(), '\n'));
            }
            return validationResult;
        } catch (Exception e) {
            log.error("Unable to parse assignment descriptor for assignment " + a.getName() + ".", e);
            return new AssignmentDescriptorValidationResult(a.getName(), null);
        }
    }

    private List<Assignment> findAssignments(Path base) {
        List<Assignment> result = new ArrayList<>();

        try {
            Files.walk(base, 1).forEach(path -> {
                try {
                    Files.walk(path, 1)
                            .filter(file -> file.getFileName().toString().equals("assignment.yaml"))
                            .forEach(file -> {
                                try {
                                    AssignmentDescriptor assignmentDescriptor = yamlObjectMapper.readValue(file.toFile(), AssignmentDescriptor.class);
                                    Assignment assignment = new Assignment();
                                    assignment.setName(assignmentDescriptor.getName());
                                    assignment.setAssignmentDescriptor(file.toAbsolutePath().toString());
                                    result.add(assignment);
                                } catch (Exception e) {
                                    throw new RuntimeException("Unable to parse assignment descriptor " + file.toString(), e);
                                }
                            });
                } catch (Exception e) {
                    throw new RuntimeException("Unable to find assignments in " + path, e);
                }
            });
        } catch (Exception e) {
            throw new AssignmentServiceException("Failed to read assignments from " + base, e);
        }
        return result;
    }

    public List<AssignmentFile> getAssignmentFiles(Assignment assignment) {
        return ASSIGNMENT_FILES.computeIfAbsent(assignment.getUuid(),
                uuid -> new JavaAssignmentFileResolver().resolve(getAssignmentDescriptor(assignment)));
    }
}
