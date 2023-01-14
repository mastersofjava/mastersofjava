package nl.moj.server.submit;

import java.nio.file.Path;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.service.CompileRequest;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.service.TestRequest;
import nl.moj.server.test.service.TestService;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmitFacade {

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final CompetitionRuntime competitionRuntime;

    private final UserService userService;

    private final CompileService compileService;

    private final TestService testService;

    private final SubmitService submitService;

    private boolean isSubmitAllowedForTeam(Team team, ActiveAssignment activeAssignment) {
        final AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment.getAssignment(), activeAssignment.getCompetitionSession(), team);
        return getRemainingSubmits(as, activeAssignment) > 0 && activeAssignment.isRunning();
    }

    private int getRemainingSubmits(AssignmentStatus as, ActiveAssignment activeAssignment) {
        int maxSubmits = activeAssignment.getAssignmentDescriptor().getScoringRules().getMaximumResubmits() + 1;
        return maxSubmits - as.getSubmitAttempts().size();
    }

    public CompileAttempt registerCompileRequest(SourceMessage message, Principal principal) {
        try {
            return compileService.registerCompileAttempt(createCompileRequest(message, principal));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    public TestAttempt registerTestRequest(SourceMessage message, Principal principal) {
        try {
            return testService.registerTestAttempt(createTestRequest(message, principal));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    public SubmitAttempt registerSubmitRequest(SourceMessage message, Principal principal) {
        try {
            return submitService.registerSubmitRequest(createSubmitRequest(message, principal));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    private SubmitRequest createSubmitRequest(SourceMessage message, Principal principal) {
        User user = userService.findUser(principal);
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment(user.getTeam(), message);
        var testCases = activeAssignment.getSubmitTestFiles();
        return SubmitRequest.builder()
                .team(user.getTeam())
                .session(activeAssignment.getCompetitionSession())
                .assignment(activeAssignment.getAssignment())
                .sources(convertSources(message.getSources(), activeAssignment))
                .tests(testCases)
                .timeElapsed(activeAssignment.getTimeElapsed())
                .isSubmitAllowed(isSubmitAllowedForTeam(user.getTeam(), activeAssignment))
                .build();
    }

    private TestRequest createTestRequest(SourceMessage message, Principal principal) {
        User user = userService.findUser(principal);
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment(user.getTeam(), message);

        var testCases = activeAssignment.getTestFiles().stream()
                .filter(t -> message.getTests().contains(t.getUuid().toString()))
                .collect(Collectors.toList());

        return TestRequest.builder()
                .team(user.getTeam())
                .session(activeAssignment.getCompetitionSession())
                .assignment(activeAssignment.getAssignment())
                .sources(convertSources(message.getSources(), activeAssignment))
                .tests(testCases)
                .build();
    }

    private CompileRequest createCompileRequest(SourceMessage message, Principal principal) {
        User user = userService.findUser(principal);
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment(user.getTeam(), message);

        return CompileRequest.builder()
                .team(user.getTeam())
                .session(activeAssignment.getCompetitionSession())
                .assignment(activeAssignment.getAssignment())
                .sources(convertSources(message.getSources(), activeAssignment))
                .build();
    }

    // incoming editable files have the uuid from the AssignmentFile, we need to translate that
    // to a relative path inside the assignment.
    private Map<Path, String> convertSources(Map<String, String> s, ActiveAssignment activeAssignment) {
        Map<UUID, String> uuidSources = s.entrySet().stream()
                .collect(Collectors.toMap(e -> UUID.fromString(e.getKey()), Map.Entry::getValue));
        Map<Path, String> sources = new HashMap<>();
        activeAssignment.getFiles().forEach(af -> {
            if (uuidSources.containsKey(af.getUuid())) {
                sources.put(af.getFile(), uuidSources.get(af.getUuid()));
            }
        });
        if (uuidSources.size() != sources.size()) {
            throw new IllegalStateException("Source conversion failed to find all expected sources!");
        }
        return sources;
    }
}
