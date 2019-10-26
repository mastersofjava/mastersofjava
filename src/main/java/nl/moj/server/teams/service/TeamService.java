package nl.moj.server.teams.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final MojServerProperties mojServerProperties;
    private final TeamRepository teamRepository;
    private final AssignmentService assignmentService;

    public Path getTeamDirectory(CompetitionSession session, Team team) {
        return mojServerProperties.getDirectories().getBaseDirectory()
                .resolve(mojServerProperties.getDirectories().getSessionDirectory())
                .resolve(session.getUuid().toString())
                .resolve(mojServerProperties.getDirectories().getTeamDirectory())
                .resolve(team.getUuid().toString());
    }

    public Path getTeamAssignmentDirectory(CompetitionSession session, Team team, Assignment assignment) {
        return getTeamDirectory(session, team).resolve(assignment.getName());
    }

    public List<Team> getTeams() {
        return teamRepository.findAllByRole(Role.USER);
    }

    public List<AssignmentFile> getTeamAssignmentFiles(CompetitionSession session, Assignment assignment, Team team) {
        List<AssignmentFile> teamFiles = new ArrayList<>();
        Path teamAssignmentBase = getTeamAssignmentDirectory(session, team, assignment).resolve("sources");

        assignmentService.getAssignmentFiles(assignment).stream()
                .filter(f -> f.getFileType().isVisible())
                .forEach(f -> {
                    Path resolvedFile = teamAssignmentBase.resolve(f.getFile());
                    if (resolvedFile.toFile().exists() && Files.isReadable(resolvedFile)) {
                        teamFiles.add(f.toBuilder()
                                .content(readPathContent(resolvedFile))
                                .build());
                    } else {
                        teamFiles.add(f.toBuilder().build());
                    }
                });
        return teamFiles;
    }

    private byte[] readPathContent(Path p) {
        try {
            return IOUtils.toByteArray(Files.newInputStream(p, StandardOpenOption.READ));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to read file '%s'.", p), e);
        }
    }
}
