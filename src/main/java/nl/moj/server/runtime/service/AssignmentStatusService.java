package nl.moj.server.runtime.service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentStatusService {

    private final AssignmentStatusRepository assignmentStatusRepository;

    @Transactional(Transactional.TxType.MANDATORY)
    public AssignmentStatus startAssignment(CompetitionSession session, Assignment assignment, Duration timeRemaining) {
        AssignmentStatus assignmentStatus = assignmentStatusRepository.findByCompetitionSessionAndAssignment(session, assignment)
                .orElseGet(() -> {
                    AssignmentStatus as = new AssignmentStatus();
                    as.setAssignment(assignment);
                    as.setCompetitionSession(session);
                    as.setDateTimeStart(Instant.now());
                    as.setTimeRemaining(timeRemaining);
                    session.getAssignmentStatuses().add(as);
                    return assignmentStatusRepository.save(as);
                });
        assignmentStatus.setDateTimeStart(Instant.now());
        assignmentStatus.setTimeRemaining(timeRemaining);
        return assignmentStatus;
    }

    @Transactional
    public AssignmentStatus updateTimeRemaining(UUID session, UUID assignment, Duration timeRemaining) {
        AssignmentStatus assignmentStatus = assignmentStatusRepository.findByCompetitionSession_UuidAndAssignment_Uuid(session, assignment)
                .orElseThrow(() -> new IllegalStateException("Could not find status for current assignment " + assignment + " in session " + session));

        assignmentStatus.setTimeRemaining(timeRemaining);
        return assignmentStatus;
    }
}
