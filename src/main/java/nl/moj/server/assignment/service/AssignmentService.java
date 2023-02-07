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

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.assignment.descriptor.ScoringRules;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.model.AssignmentDescriptorValidationResult;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.JavaAssignmentFileResolver;
import nl.moj.server.runtime.model.AssignmentFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssignmentService {

    // FIXME this is needed for now to make sure we get the same uuids as they are
    // not persisted atm.
    private static final Map<UUID, List<AssignmentFile>> ASSIGNMENT_FILES = new ConcurrentHashMap<>();

    private final ObjectMapper yamlObjectMapper;

    private final AssignmentRepository assignmentRepository;

    private final AssignmentDescriptorValidator assignmentDescriptorValidator;

    private final MojServerProperties mojServerProperties;

    public AssignmentService(@Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
                             AssignmentRepository assignmentRepository, AssignmentDescriptorValidator assignmentDescriptorValidator,
                             MojServerProperties mojServerProperties) {
        this.yamlObjectMapper = yamlObjectMapper;
        this.assignmentRepository = assignmentRepository;
        this.assignmentDescriptorValidator = assignmentDescriptorValidator;
        this.mojServerProperties = mojServerProperties;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Path getAssignmentContentFolder(UUID id) throws Exception {
        return getAssignmentContentFolder(assignmentRepository.findByUuid(id));
    }

    public Path getAssignmentContentFolder(Assignment assignment) throws IOException {
        return Path.of(assignment.getAssignmentDescriptor()).getParent();
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public AssignmentDescriptor resolveAssignmentDescriptor(UUID id) {
        return resolveAssignmentDescriptor(assignmentRepository.findByUuid(id));
    }

    public AssignmentDescriptor resolveAssignmentDescriptor(Assignment assignment) {
        return resolveAssignmentDescriptor(assignment.getAssignmentDescriptor());
    }

    public AssignmentDescriptor resolveAssignmentDescriptor(String assignmentDescriptor) {
        Path descriptor = Paths.get(assignmentDescriptor);
        AssignmentDescriptor ad = null;
        try {
            ad = yamlObjectMapper.readValue(Files.newInputStream(descriptor),
                    AssignmentDescriptor.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ad.setDirectory(descriptor.getParent());
        ad.setOriginalAssignmentDescriptor(assignmentDescriptor);
        return ad;
    }

    public List<Assignment> updateAssignments() throws IOException, AssignmentServiceException {
        return updateAssignments(mojServerProperties.getAssignmentRepo(), null);

    }

    public List<Assignment> updateAssignments(Path base, String collection) throws IOException, AssignmentServiceException {
        log.info("Discovering assignments from {}.", base);
        List<Assignment> assignments = scanAssignments(base);

        // validate
        List<String> invalid = assignments.stream().map(this::validateAssignment).filter(r -> !r.isValid())
                .map(AssignmentDescriptorValidationResult::getAssignment).toList();
        if (invalid.isEmpty()) {
            ASSIGNMENT_FILES.clear();
            // update or create
            return assignments.stream().map(d -> {
                Assignment current = assignmentRepository.findByName(d.getName());
                if (current != null) {
                    log.info("Updating existing assignment {}.", current.getName());
                    current.setAssignmentDescriptor(d.getAssignmentDescriptor());
                    current.setAllowedSubmits(d.getAllowedSubmits());
                    current.setAssignmentDuration(d.getAssignmentDuration());
                    current.setCollection(StringUtils.isNotBlank(collection) ? collection : d.getCollection());
                    return assignmentRepository.save(current);
                } else {
                    log.info("Added new assignment {}.", d.getName());
                    Assignment a = new Assignment();
                    a.setAssignmentDescriptor(d.getAssignmentDescriptor());
                    a.setName(d.getName());
                    a.setUuid(UUID.randomUUID());
                    a.setAssignmentDuration(d.getAssignmentDuration());
                    a.setAllowedSubmits(d.getAllowedSubmits());
                    a.setCollection(StringUtils.isNotBlank(collection) ? collection : d.getCollection());
                    return assignmentRepository.save(a);
                }
            }).collect(Collectors.toList());
        } else {
            throw new AssignmentServiceException("Problems during assignment update of assignment(s) '"
                    + Strings.join(invalid, ',') + "' see the logs for information. Correct problems and try again.");
        }
    }

    public List<AssignmentFile> getAssignmentFiles(Assignment assignment) {
        return getAssignmentFiles(assignment.getUuid(), assignment.getAssignmentDescriptor());
    }

    public List<AssignmentFile> getAssignmentFiles(UUID uuid) throws IOException {
        Assignment assignment = assignmentRepository.findByUuid(uuid);
        if (assignment == null) {
            return Collections.emptyList();
        }
        return getAssignmentFiles(assignment);
    }

    private List<AssignmentFile> getAssignmentFiles(UUID uuid, String assignmentDescriptor) {
        if (!ASSIGNMENT_FILES.containsKey(uuid)) {
            ASSIGNMENT_FILES.put(uuid, new JavaAssignmentFileResolver().resolve(resolveAssignmentDescriptor(assignmentDescriptor)));
        }
        return ASSIGNMENT_FILES.get(uuid);
    }

    private AssignmentDescriptorValidationResult validateAssignment(Assignment a) {
        // check if assignment descriptor can be read.
        try {
            AssignmentDescriptor ad = resolveAssignmentDescriptor(a);
            AssignmentDescriptorValidationResult validationResult = assignmentDescriptorValidator.validate(ad);
            if (!validationResult.isValid()) {
                log.error("Validation of assignment {} failed. Problems found: \n{}", a.getName(),
                        Strings.join(validationResult.getValidationMessages(), '\n'));
            }
            return validationResult;
        } catch (Exception e) {
            log.error("Unable to parse assignment descriptor for assignment " + a.getName() + ".", e);
            return new AssignmentDescriptorValidationResult(a.getName(), null);
        }
    }

    private List<Assignment> scanAssignments(Path base) throws IOException {
        List<Assignment> result = new ArrayList<>();
        try (Stream<Path> files = Files.walk(base)) {
            files.filter(this::isAssignmentDescriptor)
                    .forEach(file -> {
                        try {
                            AssignmentDescriptor assignmentDescriptor = yamlObjectMapper
                                    .readValue(file.toFile(), AssignmentDescriptor.class);
                            ScoringRules scoringRules = assignmentDescriptor.getScoringRules();
                            Assignment assignment = new Assignment();
                            assignment.setName(assignmentDescriptor.getName());
                            assignment.setAssignmentDescriptor(file.toAbsolutePath().toString());
                            assignment.setAssignmentDuration(assignmentDescriptor.getDuration());
                            assignment.setAllowedSubmits(scoringRules.getMaximumResubmits() != null ? scoringRules.getMaximumResubmits() + 1 : 1);
                            assignment.setCollection(file.getName(base.getNameCount()).toString());
                            result.add(assignment);
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Unable to parse assignment descriptor " + file.toString(), e);
                        }
                    });
        }
        return result;
    }

    private boolean isAssignmentDescriptor(Path f) {
        return f.getFileName().toString().equals("assignment.yaml")
                || f.getFileName().toString().equals("assignment.yml");
    }
}
