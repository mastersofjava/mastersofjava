package nl.moj.server.competition.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionCleaningService {
    private final CompetitionRuntime competition;

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final AssignmentResultRepository assignmentResultRepository;

    private final CompileAttemptRepository compileAttemptRepository;

    private final TestCaseRepository testCasesRepository;

    private final TestAttemptRepository testAttemptRepository;

    private final SubmitAttemptRepository submitAttemptRepository;

    private final CompetitionSessionRepository competitionSessionRepository;

    public String doCleanComplete() {
        competition.getCompetitionState().getCompletedAssignments().clear();
        if (assignmentStatusRepository.count()==0) {
            return "competition not started yet";
        }
        compileAttemptRepository.deleteAll();
        testCasesRepository.deleteAll();
        testAttemptRepository.deleteAll();
        submitAttemptRepository.deleteAll();
        competitionSessionRepository.deleteAll();
        assignmentStatusRepository.deleteAll();// correct cleaning: first delete all status items, afterwards delete all results
        assignmentResultRepository.deleteAll();
        return "competition restarted, reloading page";
    }
}
