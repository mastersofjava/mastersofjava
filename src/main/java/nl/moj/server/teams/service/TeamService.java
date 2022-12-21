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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final MojServerProperties mojServerProperties;
    private final TeamRepository teamRepository;
    private final AssignmentService assignmentService;

    public Path getTeamDirectory(CompetitionSession session, UUID teamUUID) {
        return mojServerProperties.getDirectories().getBaseDirectory()
                .resolve(mojServerProperties.getDirectories().getSessionDirectory())
                .resolve(session.getUuid().toString())
                .resolve(mojServerProperties.getDirectories().getTeamDirectory())
                .resolve(teamUUID.toString());
    }

    public Path getTeamAssignmentDirectory(CompetitionSession session, UUID teamUUID, Assignment assignment) {
        return getTeamDirectory(session, teamUUID).resolve(assignment.getName());
    }

    public List<Team> getTeams() {
        return teamRepository.findAll();
    }

    public List<AssignmentFile> getTeamAssignmentFiles(CompetitionSession session, Assignment assignment, UUID teamUUID) {
        List<AssignmentFile> teamFiles = new ArrayList<>();
        Path teamAssignmentBase = getTeamAssignmentDirectory(session,  teamUUID, assignment).resolve("sources");

        assignmentService.getAssignmentFiles(assignment).stream()
                .filter(f -> f.getFileType().isVisible())
                .forEach(f -> {
                    Path resolvedFile = teamAssignmentBase.resolve(f.getFile());
                    log.info("resolvedFile " +resolvedFile.toFile().getAbsoluteFile());
                    if (resolvedFile.toFile().exists() && Files.isReadable(resolvedFile)) {
                        teamFiles.add(f.toBuilder()
                                .content(readPathContent(resolvedFile))
                                .build());
                    } else if( f.getFileType().isContentHidden()){
                        teamFiles.add(f.toBuilder().content("-- content intentionally hidden --".getBytes(StandardCharsets.UTF_8)).build());
                    } else {
                        teamFiles.add(f.toBuilder().build());
                    }
                });
        return teamFiles;
    }

    public Team createTeam(String name, String company, String country) {
        Team t = teamRepository.findByName(name);
        if( t == null ) {
            t = teamRepository.save(Team.builder()
                    .company(company)
                    .name(name)
                    .country(country)
                    .uuid(UUID.randomUUID())
                    .build());
        }
        return t;
    }

    private byte[] readPathContent(Path p) {
        try {
            return IOUtils.toByteArray(Files.newInputStream(p, StandardOpenOption.READ));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read file '%s'.", p), e);
        }
    }
}
