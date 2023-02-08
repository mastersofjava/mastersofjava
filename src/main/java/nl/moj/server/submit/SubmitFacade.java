package nl.moj.server.submit;

import java.nio.file.Path;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.service.CompileRequest;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.service.TestRequest;
import nl.moj.server.test.service.TestService;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmitFacade {
    private final CompetitionRuntime competitionRuntime;

    private final UserService userService;

    private final CompileService compileService;

    private final TestService testService;

    private final SubmitService submitService;

    private final AssignmentRepository assignmentRepository;

    @Transactional
    public CompileAttempt registerCompileRequest(SourceMessage message, Principal principal) {
        try {
            return compileService.registerCompileAttempt(createCompileRequest(message, principal));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Transactional
    public TestAttempt registerTestRequest(SourceMessage message, Principal principal) {
        try {
            return testService.registerTestAttempt(createTestRequest(message, principal));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Transactional
    public SubmitAttempt registerSubmitRequest(SourceMessage message, Principal principal) {
        try {
            return submitService.registerSubmitAttempt(createSubmitRequest(message, principal));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    private SubmitRequest createSubmitRequest(SourceMessage message, Principal principal) {
        User user = userService.findUser(principal);

        // TODO we rely on volatile game state here, need to fix this
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment();

        // in case games state has no assigment load using the name.
        Assignment assignment = activeAssignment.getAssignment();
        if( activeAssignment.getCompetitionSession() != null && assignment == null) {
            assignment = assignmentRepository.findByName(message.getAssignmentName());
        }

        var testCases = activeAssignment.getSubmitTestFiles();
        return SubmitRequest.builder()
                .team(user.getTeam())
                .session(activeAssignment.getCompetitionSession())
                .assignment(assignment)
                .sources(convertSources(message.getSources(), activeAssignment))
                .tests(testCases)
                .build();
    }

    private TestRequest createTestRequest(SourceMessage message, Principal principal) {
        User user = userService.findUser(principal);
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment();

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
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment();

        return CompileRequest.builder()
                .team(user.getTeam())
                .session(activeAssignment.getCompetitionSession())
                .assignment(activeAssignment.getAssignment())
                .sources(convertSources(message.getSources(), activeAssignment))
                .build();
    }

    // TODO fix this!
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
