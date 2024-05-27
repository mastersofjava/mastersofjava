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
package nl.moj.server.runtime;

import static nl.moj.server.TestUtil.classpathResourceToPath;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.bootstrap.BootstrapService;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.DbUtil;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.CompetitionSession.SessionType;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.support.TestJmsListener;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.user.model.User;
import nl.moj.server.user.repository.UserRepository;
import nl.moj.server.util.PathUtil;
import nl.moj.server.util.TransactionHelper;

@Slf4j
@DirtiesContext
public abstract class BaseRuntimeTest {

    @Autowired
    private TestJmsListener mockJmsService;

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
    private UserRepository userRepository;

    @Autowired
    private MojServerProperties mojServerProperties;

    @Autowired
    private BootstrapService bootstrapService;

    @Autowired
    private CompileAttemptRepository compileAttemptRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private SubmitAttemptRepository submitAttemptRepository;

    @Autowired
    private TeamAssignmentStatusRepository teamAssignmentStatusRepository;

    @Autowired
    private CompetitionSessionRepository competitionSessionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TransactionHelper trx;

    @Getter
    private Competition competition;

    @Getter
    private UUID sessionId;

    @Getter
    private Team team;

    @Getter
    private User user;

    @BeforeEach
    public void init() throws Exception {
        try {
            bootstrapService.bootstrap();
            dbUtil.cleanup();
            competition = createCompetition();
            sessionId = competitionRuntime.startSession(competition.getUuid(), SessionType.GROUP).getUuid();
            mockJmsService.reset();
        } catch (NullPointerException npe) {
            log.error("Nullpointer: {}", npe.getMessage(), npe);
            throw npe;
        } catch (DataIntegrityViolationException dive) {
            log.error("DB cleaning failed {}", dive.getMessage(), dive);
        }
    }

    @Transactional
    public CompileAttempt refresh(CompileAttempt entity) {
        return compileAttemptRepository.findById(entity.getId()).orElse(null);
    }

    @Transactional
    public TestAttempt refresh(TestAttempt entity) {
        return testAttemptRepository.findById(entity.getId()).orElse(null);
    }

    @Transactional
    public SubmitAttempt refresh(SubmitAttempt entity) {
        return submitAttemptRepository.findById(entity.getId()).orElse(null);
    }

    public boolean awaitAttempt(UUID attempt, long timeout, TimeUnit unit) {
        try {
            return mockJmsService.awaitAttempt(attempt, timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void cleanup() throws Exception {
        try {
            if (competitionRuntime.getCurrentAssignment() != null) {
                competitionRuntime.stopAssignment(sessionId, competitionRuntime.getCurrentAssignment());
            }
            dbUtil.cleanup();
            mockJmsService.reset();
            PathUtil.delete(mojServerProperties.getDataDirectory(), true);
        } catch (NullPointerException npe) {
            log.error("Nullpointer: {}", npe.getMessage(), npe);
            throw npe;
        } catch (DataIntegrityViolationException dive) {
            log.error("DB cleaning failed {}", dive.getMessage(), dive);
        }
    }

    protected Principal getPrincipal(User user) {
        return user::getName;
    }

    protected User addUser(Team team) {
        User user = new User();
        user.setTeam(team);
        user.setName("username-" + team.getId());
        user.setGivenName("User");
        user.setFamilyName("Name");
        user.setEmail("user.name@example.com");
        return userRepository.save(user);
    }

    protected Team addTeam() {
        Team team = new Team();
        team.setUuid(UUID.randomUUID());
        team.setName(team.getUuid().toString());
        return teamRepository.save(team);
    }

    private Competition createCompetition() throws Exception {
        team = addTeam();
        user = addUser(team);

        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/runtime/assignments"),
                "runtime");
        AtomicInteger count = new AtomicInteger(0);
        final Competition c = new Competition();
        c.setUuid(UUID.randomUUID());
        c.setName("TestCase");
        c.setAssignments(assignments.stream()
                .map(a -> {
                    CompetitionAssignment oa = new CompetitionAssignment();
                    oa.setCompetition(c);
                    oa.setAssignment(a);
                    oa.setOrder(count.getAndIncrement());
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

    protected CompetitionAssignment getAssignment(String name) {
        Assert.isTrue(name != null, "invalid name used");
        return getCompetition().getAssignments()
                .stream()
                .filter(a -> a.getAssignment().getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    protected List<ExecutionWindow> getExecutionWindows(SubmitAttempt... sa) {
        return Arrays.stream(sa).flatMap(s -> getExecutionWindows(s.getTestAttempt()).stream()).toList();
    }

    protected List<ExecutionWindow> getExecutionWindows(TestAttempt... tas) {
        return trx.required(() -> Arrays.stream(tas).flatMap(ta -> {
            TestAttempt r = refresh(ta);
            return Stream.concat(Stream.of(ExecutionWindow.builder()
                    .start(r.getCompileAttempt().getDateTimeStart())
                    .end(r.getCompileAttempt().getDateTimeEnd())
                    .worker(r.getCompileAttempt().getWorker())
                    .type("compile")
                    .build()),
                    r.getTestCases()
                            .stream()
                            .map(tr -> ExecutionWindow.builder()
                                    .start(tr.getDateTimeStart())
                                    .end(tr.getDateTimeEnd())
                                    .worker(tr.getWorker())
                                    .type("test-case")
                                    .build()));
        }).toList());
    }

    protected void assertNoOverlappingExecutionWindows(TestAttempt... results) {
        assertNoOverlappingExecutionWindows(getExecutionWindows(results));
    }

    protected void assertOverlappingExecutionWindows(TestAttempt... results) {
        assertOverlappingExecutionWindows(getExecutionWindows(results));
    }

    private void assertNoOverlappingExecutionWindows(List<ExecutionWindow> windows) {
        Assertions.assertThat(isOverlappingExecutionWindows(windows)).isFalse();
    }

    private void assertOverlappingExecutionWindows(List<ExecutionWindow> windows) {
        Assertions.assertThat(isOverlappingExecutionWindows(windows)).isTrue();
    }

    private boolean isOverlappingExecutionWindows(List<ExecutionWindow> windows) {
        List<ExecutionWindow> sorted = new ArrayList<>(windows);
        sorted.sort(Comparator.comparing(ExecutionWindow::getStart));
        ExecutionWindow previous = null;
        boolean overlap = false;
        for (ExecutionWindow w : sorted) {
            if (previous != null && previous.getWorker().equals(w.getWorker())) {
                overlap = overlap || w.getStart().isBefore(previous.getEnd());
            }
            previous = w;
        }
        return overlap;
    }

    protected AbstractLongAssert<?> assertFinalScore(SubmitAttempt sa) {
        return trx.required(() -> {
            SubmitAttempt r = refresh(sa);
            Assertions.assertThat(r.getAssignmentStatus()).isNotNull();
            Assertions.assertThat(r.getAssignmentStatus().getAssignmentResult()).isNotNull();
            return Assertions.assertThat(r.getAssignmentStatus().getAssignmentResult().getFinalScore());
        });
    }

    protected AbstractLongAssert<?> assertFinalScore(SourceMessage src) {
        return trx.required(() -> {
            Assignment assignment = assignmentRepository.findByName(src.getAssignmentName());
            CompetitionSession session = competitionSessionRepository.findByUuid(getSessionId());
            Team team = teamRepository.findByUuid(UUID.fromString(src.getUuid()));
            TeamAssignmentStatus tas = teamAssignmentStatusRepository
                    .findByAssignmentAndCompetitionSessionAndTeam(assignment, session, team).orElse(null);

            Assertions.assertThat(tas).isNotNull();
            AssignmentResult r = tas.getAssignmentResult();

            Assertions.assertThat(r).isNotNull();
            return Assertions.assertThat(r.getFinalScore());
        });
    }

    protected void assertSuccess(SubmitAttempt sa) {
        trx.required(() -> {
            SubmitAttempt r = refresh(sa);
            assertSuccess(r.getTestAttempt());
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getSuccess()).isTrue();
            Assertions.assertThat(r.getAborted()).isFalse();
            Assertions.assertThat(r.getAssignmentStatus()).isNotNull();
            Assertions.assertThat(r.getAssignmentStatus().getAssignmentResult()).isNotNull();
        });
    }

    protected void assertTimeout(SubmitAttempt sa) {
        trx.required(() -> {
            SubmitAttempt r = refresh(sa);
            assertSuccess(r.getTestAttempt());
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getSuccess()).isTrue();
            Assertions.assertThat(r.getAborted()).isFalse();
            Assertions.assertThat(r.getAssignmentStatus()).isNotNull();
            Assertions.assertThat(r.getAssignmentStatus().getAssignmentResult()).isNotNull();
        });
    }

    protected void assertSuccess(TestAttempt ta) {
        trx.required(() -> {
            TestAttempt r = testAttemptRepository.findByUuid(ta.getUuid());
            assertSuccess(r.getCompileAttempt());
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getWorker()).isNotNull();
            Assertions.assertThat(r.getTrace()).isNotNull();
            Assertions.assertThat(r.getTestCases())
                    .allMatch(tc -> tc.getSuccess() && !tc.getTimeout() && !tc.getAborted());
        });
    }

    protected void assertTimeout(TestAttempt ta) {
        trx.required(() -> {
            TestAttempt r = refresh(ta);
            assertSuccess(r.getCompileAttempt());
            Assertions.assertThat(r.getWorker()).isNotNull();
            Assertions.assertThat(r.getTrace()).isNotNull();
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getTestCases())
                    .anyMatch(tc -> !tc.getSuccess() && tc.getTimeout() && !tc.getAborted());
        });
    }

    protected void assertSuccess(CompileAttempt ca) {
        trx.required(() -> {
            CompileAttempt r = refresh(ca);
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getWorker()).isNotNull();
            Assertions.assertThat(r.getTrace()).isNotNull();
            Assertions.assertThat(r.getSuccess()).isTrue();
            Assertions.assertThat(r.getTimeout()).isFalse();
            Assertions.assertThat(r.getAborted()).isFalse();
        });
    }

    protected void assertTimeout(CompileAttempt ca) {
        trx.required(() -> {
            CompileAttempt r = refresh(ca);
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getWorker()).isNotNull();
            Assertions.assertThat(r.getTrace()).isNotNull();
            Assertions.assertThat(r.getSuccess()).isFalse();
            Assertions.assertThat(r.getTimeout()).isTrue();
            Assertions.assertThat(r.getAborted()).isFalse();
        });
    }

    protected void assertAbort(CompileAttempt ca) {
        trx.required(() -> {
            CompileAttempt r = refresh(ca);
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getSuccess()).isFalse();
            Assertions.assertThat(r.getTimeout()).isFalse();
            Assertions.assertThat(r.getAborted()).isTrue();
        });
    }

    protected void assertAbort(TestAttempt ta) {
        trx.required(() -> {
            TestAttempt r = refresh(ta);
            assertAbort(r.getCompileAttempt());
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getAborted()).isTrue();

            r.getTestCases().forEach(tc -> {
                Assertions.assertThat(tc.getAborted()).isTrue();
                Assertions.assertThat(tc.getSuccess()).isFalse();
                Assertions.assertThat(tc.getTimeout()).isFalse();
            });

        });
    }

    protected void assertAbort(SubmitAttempt sa) {
        trx.required(() -> {
            SubmitAttempt r = refresh(sa);
            assertAbort(r.getTestAttempt());
            Assertions.assertThat(r).isNotNull();
            Assertions.assertThat(r.getSuccess()).isFalse();
            Assertions.assertThat(r.getAborted()).isTrue();
        });
    }

    @Value
    @Builder
    public static class ExecutionWindow {
        Instant start;
        Instant end;
        String worker;
        String type;

        public Duration toDuration() {
            return Duration.between(start, end);
        }
    }
}
