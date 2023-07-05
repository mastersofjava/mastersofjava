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
package nl.moj.server.teams.service;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.storage.StorageService;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final AssignmentService assignmentService;
    private final AssignmentRepository assignmentRepository;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final StorageService storageService;

    public Path getTeamAssignmentDirectory(UUID teamId, UUID sessionId, String assignmentName) {
        return storageService.getSessionTeamFolder(sessionId, teamId).resolve(assignmentName);
    }

    public List<Team> getTeams() {
        return teamRepository.findAll();
    }

    public void updateAssignment(UUID teamId, UUID sessionId, UUID assignmentId, Map<Path, String> content) throws IOException {
        // TODO assignment directory is based on name, files are stored based on UUID need to resolve this
        Path teamAssignmentBase = getTeamAssignmentDirectory(teamId, sessionId, assignmentRepository.findByUuid(assignmentId)
                .getName()).resolve("sources");

        for (Map.Entry<Path, String> entry : content.entrySet()) {
            Path taf = teamAssignmentBase.resolve(entry.getKey());
            Files.createDirectories(taf.getParent());
            Files.copy(new ByteArrayInputStream(entry.getValue()
                    .getBytes(StandardCharsets.UTF_8)), taf, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void cleanAssignment(UUID teamId, UUID sessionId, UUID assignmentId) throws IOException {
        Path teamAssignmentBase = getTeamAssignmentDirectory(teamId, sessionId, assignmentRepository.findByUuid(assignmentId)
                .getName()).resolve("sources");
        try (Stream<Path> walk = Files.walk(teamAssignmentBase)) {
            walk.sorted(Comparator.reverseOrder()).forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public List<AssignmentFile> getTeamAssignmentFiles(UUID teamId, UUID sessionId, UUID assignmentId) {
        List<AssignmentFile> teamFiles = new ArrayList<>();

        // TODO assignment directory is based on name, files are stored based on UUID need to resolve this
        Path teamAssignmentBase = getTeamAssignmentDirectory(teamId, sessionId, assignmentRepository.findByUuid(assignmentId)
                .getName()).resolve("sources");

        assignmentService.getAssignmentFiles(assignmentId).stream()
                .filter(f -> f.getFileType().isVisible())
                .forEach(f -> {
                    Path resolvedFile = teamAssignmentBase.resolve(f.getFile());
                    log.info("resolvedFile " + resolvedFile.toFile().getAbsoluteFile());
                    if (resolvedFile.toFile().exists() && Files.isReadable(resolvedFile)) {
                        teamFiles.add(f.toBuilder()
                                .content(readPathContent(resolvedFile))
                                .build());
                    } else if (f.getFileType().isContentHidden()) {
                        teamFiles.add(f.toBuilder()
                                .content("-- content intentionally hidden --".getBytes(StandardCharsets.UTF_8))
                                .build());
                    } else {
                        teamFiles.add(f.toBuilder().build());
                    }
                });
        return teamFiles;
    }

    @Transactional
    public Team createOrUpdate(String name, String company, String country) {
        Team t = teamRepository.findByName(name);
        if (t == null) {
            t = teamRepository.save(Team.builder()
                    .name(name)
                    .uuid(UUID.randomUUID())
                    .build());
        }
        t.setCompany(company);
        t.setCountry(country);
        return t;
    }

    private byte[] readPathContent(Path p) {
        try {
            return IOUtils.toByteArray(Files.newInputStream(p, StandardOpenOption.READ));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read file '%s'.", p), e);
        }
    }

    @Transactional
    public boolean deleteTeam(String name) {
        Team team = teamRepository.findByName(name);
        if (team == null) {
            return false;
        }

        teamAssignmentStatusRepository.deleteAll(teamAssignmentStatusRepository.findByTeam(team));
        teamRepository.delete(team);
        return true;
    }
}
