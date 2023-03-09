package nl.moj.server.test.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.compiler.service.CompileRequest;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.teams.model.Team;
import nl.moj.server.user.model.User;

@Data
@Builder
public class TestRequest {

    private final Team team;
    private final Assignment assignment;
    private final CompetitionSession session;
    private final List<AssignmentFile> tests;
    private final Map<Path,String> sources;
}
