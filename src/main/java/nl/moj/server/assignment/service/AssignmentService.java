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

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.assignment.descriptor.ScoringRules;
import nl.moj.common.assignment.service.AssignmentDescriptorService;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.common.storage.StorageService;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.model.AssignmentDescriptorValidationResult;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.runtime.JavaAssignmentFileResolver;
import nl.moj.server.runtime.model.AssignmentFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    // FIXME this is needed for now to make sure we get the same uuids as they are
    // not persisted atm.
    private static final Map<UUID, List<AssignmentFile>> ASSIGNMENT_FILES = new ConcurrentHashMap<>();

    private final AssignmentRepository assignmentRepository;

    private final AssignmentDescriptorValidator assignmentDescriptorValidator;

    private final AssignmentDescriptorService assignmentDescriptorService;

    private final MojServerProperties mojServerProperties;

    private final StorageService storageService;

    @Transactional(Transactional.TxType.REQUIRED)
    public Assignment findAssignmentByName(String name) {
        return assignmentRepository.findByName(name);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Path getAssignmentContentFolder(UUID id) {
        return getAssignmentContentFolder(assignmentRepository.findByUuid(id));
    }

    public Path getAssignmentContentFolder(Assignment assignment) {
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
        return assignmentDescriptorService.parseAssignmentDescriptor(Paths.get(assignmentDescriptor));
    }

    public Duration resolveCompileAbortTimout(Assignment assignment) {
        AssignmentDescriptor ad = resolveAssignmentDescriptor(assignment);
        return resolveCompileAbortTimout(ad).plusSeconds(5);
    }

    private Duration resolveCompileAbortTimout(AssignmentDescriptor ad) {
        return ad.getCompileTimeout() != null ? ad.getCompileTimeout()
                : mojServerProperties.getLimits().getCompileTimeout();
    }

    public Duration resolveTestAbortTimout(Assignment assignment, int numberOfTests) {
        AssignmentDescriptor ad = resolveAssignmentDescriptor(assignment);
        return resolveTestAbortTimout(ad, numberOfTests).plusSeconds(5);
    }

    private Duration resolveTestAbortTimout(AssignmentDescriptor ad, int numberOfTests) {
        Duration timeout = ad.getTestTimeout() != null ? ad.getTestTimeout()
                : mojServerProperties.getLimits().getTestTimeout();
        return resolveCompileAbortTimout(ad).plus(timeout.multipliedBy(numberOfTests));
    }

    public Duration resolveSubmitAbortTimout(Assignment assignment) {
        AssignmentDescriptor ad = resolveAssignmentDescriptor(assignment);
        return resolveTestAbortTimout(ad, ad.getAssignmentFiles().getTestSources().getTotalTestCount()).plusSeconds(5);
    }

    public List<Assignment> updateAssignments() throws IOException, AssignmentServiceException {
        return updateAssignments(storageService.getAssignmentsFolder(), null);

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
        return getAssignmentFiles(assignment.getUuid());
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public List<AssignmentFile> getAssignmentFiles(UUID uuid) {
        Assignment assignment = assignmentRepository.findByUuid(uuid);
        if (assignment == null) {
            return Collections.emptyList();
        }
        if (!ASSIGNMENT_FILES.containsKey(uuid)) {
            ASSIGNMENT_FILES.put(uuid, new JavaAssignmentFileResolver()
                    .resolve(resolveAssignmentDescriptor(assignment.getAssignmentDescriptor())));
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
        try (Stream<Path> files = Files.walk(base, FileVisitOption.FOLLOW_LINKS)) {
            files.filter(assignmentDescriptorService::isAssignmentDescriptor)
                    .forEach(file -> {
                        try {
                            AssignmentDescriptor assignmentDescriptor = assignmentDescriptorService
                                    .parseAssignmentDescriptor(file);
                            ScoringRules scoringRules = assignmentDescriptor.getScoringRules();
                            Assignment assignment = new Assignment();
                            assignment.setName(assignmentDescriptor.getName());
                            assignment.setAssignmentDescriptor(file.toAbsolutePath().toString());
                            assignment.setAssignmentDuration(assignmentDescriptor.getDuration());
                            assignment.setAllowedSubmits(
                                    scoringRules.getMaximumResubmits() != null ? scoringRules.getMaximumResubmits() + 1 : 1);
                            assignment.setCollection(file.getName(base.getNameCount()).toString());
                            result.add(assignment);
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Unable to parse assignment descriptor " + file, e);
                        }
                    });
        }
        return result;
    }

}
