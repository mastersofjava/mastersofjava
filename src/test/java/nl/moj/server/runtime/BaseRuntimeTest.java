package nl.moj.server.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.DbUtil;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.bootstrap.service.BootstrapService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.PathUtil;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import static nl.moj.server.TestUtil.classpathResourceToPath;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public abstract class BaseRuntimeTest {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private DbUtil dbUtil;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MojServerProperties mojServerProperties;

    @Autowired
    private BootstrapService bootstrapService;

    @Getter
    private Competition competition;

    @Getter
    private Team team;

    @Before
    public void init() throws IOException {
        try {
            bootstrapService.bootstrap("admin", "admin");
            dbUtil.cleanup();
            competition = createCompetition();
            competitionRuntime.startSession(competition);
        } catch (NullPointerException npe) {
            log.error("Nullpointer: {}", npe.getMessage(), npe);
            throw npe;
        }
    }

    @After
    public void cleanup() throws IOException {
        dbUtil.cleanup();
        PathUtil.delete(mojServerProperties.getDirectories().getBaseDirectory(), true);
    }

    protected Team addTeam() {
        Team team = new Team();
        team.setUuid(UUID.randomUUID());
        team.setName(team.getUuid().toString());
        team.setRole(Role.USER);
        return teamRepository.save(team);
    }

    private Competition createCompetition() {
        team = addTeam();

        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/runtime/assignments"));
        AtomicInteger count = new AtomicInteger(0);
        final Competition c = new Competition();
        c.setUuid(UUID.randomUUID());
        c.setName("TestCase");
        c.setAssignments(assignments.stream()
                .map(a -> {
                    OrderedAssignment oa = new OrderedAssignment();
                    oa.setCompetition(c);
                    oa.setAssignment(a);
                    oa.setOrder(count.getAndIncrement());
                    oa.setUuid(UUID.randomUUID());
                    return oa;
                }).collect(Collectors.toList()));

        return competitionRepository.save(c);
    }

    protected Map<String, String> getAssignmentFiles(ActiveAssignment state, Map<String, String> values) {
        return state.getAssignmentFiles().stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT)
                .map(f -> replaceValues(f, values))
                .collect(Collectors.toMap(f -> f.getUuid().toString(), AssignmentFile::getContentAsString));
    }

    private AssignmentFile replaceValues(AssignmentFile file, Map<String, String> values) {
        String content = file.getContentAsString();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            content = content.replaceAll("\\{" + entry.getKey() + "}", entry.getValue());
        }
        return file.toBuilder().content(content.getBytes(StandardCharsets.UTF_8)).build();
    }

    protected OrderedAssignment getAssignment(String name) {
        return getCompetition().getAssignments()
                .stream()
                .filter(a -> a.getAssignment().getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    protected Stream<ExecutionWindow> getExecutionWindows(SubmitResult... sr) {
        return Arrays.stream(sr).flatMap(r ->
                Stream.concat(Stream.of(ExecutionWindow.builder()
                                .start(r.getCompileResult().getDateTimeStart())
                                .end(r.getCompileResult().getDateTimeEnd())
                                .build()),
                        r.getTestResults()
                                .getResults()
                                .stream()
                                .map(tr -> ExecutionWindow.builder()
                                        .start(tr.getDateTimeStart())
                                        .end(tr.getDateTimeEnd())
                                        .build())));
    }

    protected void assertNoOverlappingExecutionWindows(SubmitResult ... results) {
        List<ExecutionWindow> windows = getExecutionWindows(results).sorted(Comparator.comparing(ExecutionWindow::getStart))
                .collect(Collectors.toList());
        ExecutionWindow previous = null;
        for( ExecutionWindow w : windows ) {
            if( previous != null) {
                assertThat(w.getStart()).isAfter(previous.getEnd());
            }
            previous = w;
        }
    }

    @Value
    @Builder
    public static class ExecutionWindow {
        private Instant start;
        private Instant end;

        public Duration toDuration() {
            return Duration.between(start, end);
        }
    }
}
