package nl.moj.server.competition.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionCleaningService {
    private final CompetitionRuntime competitionRuntime;

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final AssignmentResultRepository assignmentResultRepository;

    private final CompileAttemptRepository compileAttemptRepository;

    private final TestCaseRepository testCasesRepository;

    private final TestAttemptRepository testAttemptRepository;

    private final SubmitAttemptRepository submitAttemptRepository;

    private final CompetitionSessionRepository competitionSessionRepository;

    public String doCleanComplete(CompetitionSession competitionSession) {
        competitionRuntime.getCompetitionState().getCompletedAssignments().clear();
        if (assignmentStatusRepository.count()==0) {
            return "competition not started yet";
        }
        log.info("delete contents of session " + competitionSession.getId());

        List<AssignmentStatus> statusList = assignmentStatusRepository.findByCompetitionSession(competitionSession);
        List<AssignmentResult> resultList = assignmentResultRepository.findByCompetitionSession(competitionSession);
        log.info("contents of session " + statusList.size() + " " +resultList.size() );

        try {
            competitionSession.setDateTimeStart(null);
            competitionSession.setRunning(false);
            competitionSession.setAssignmentName(null);
            competitionSession.setTimeLeft(null);
            competitionSessionRepository.save(competitionSession);
            for (AssignmentResult result: resultList) {
                assignmentResultRepository.delete(result);
            }
            for (AssignmentStatus status: statusList) {
                List<SubmitAttempt> saList = submitAttemptRepository.findByAssignmentStatus(status);

                for (SubmitAttempt sa: saList) {
                    submitAttemptRepository.delete(sa);
                }

                List<CompileAttempt> caList = compileAttemptRepository.findByAssignmentStatus(status);
                for (CompileAttempt ca: caList) {
                    compileAttemptRepository.delete(ca);
                }
                List<TestAttempt> taList =  testAttemptRepository.findByAssignmentStatus(status);
                for (TestAttempt ta: taList) {
                    List<TestCase> tcList= testCasesRepository.findByTestAttempt(ta);
                    for (TestCase tc: tcList) {
                        testCasesRepository.delete(tc);
                    }
                    testAttemptRepository.delete(ta);
                }
                assignmentStatusRepository.delete(status);
            }

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

       // correct cleaning: first delete all status items, afterwards delete all results

        return "competition restarted, reloading page";
    }
}
