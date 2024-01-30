package nl.moj.server.test.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.teams.model.Team;

@Data
@Builder
public class TestRequest {

    private final Team team;
    private final Assignment assignment;
    private final CompetitionSession session;
    private final List<AssignmentFile> tests;
    private final Map<Path, String> sources;
}
