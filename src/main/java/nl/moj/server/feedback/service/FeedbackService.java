package nl.moj.server.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.feedback.model.TeamFeedback;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.model.TestAttempt;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final TeamRepository teamRepository;
    private final AssignmentRepository assignmentRepository;
    private final CompetitionSessionRepository competitionSessionRepository;

    @Transactional(Transactional.TxType.REQUIRED)
    public List<TeamFeedback> getAssignmentFeedback(UUID sessionId, UUID assignmentId) {
        return getAssignmentFeedback(assignmentRepository.findByUuid(assignmentId), competitionSessionRepository.findByUuid(sessionId));
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public List<TeamFeedback> getAssignmentFeedback(Assignment assignment, CompetitionSession session) {
        List<Team> allTeams = teamRepository.findAll();

        if (session == null) {
            return Collections.emptyList();
        }
        if (assignment == null) {
            if (session.getSessionType() == CompetitionSession.SessionType.GROUP) {
                return allTeams.stream().map(t -> TeamFeedback.builder().team(t).build()).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        List<TeamAssignmentStatus> statuses = teamAssignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, session);
        List<TeamFeedback> results = new ArrayList<>();
        for (Team t : allTeams) {
            Optional<TeamAssignmentStatus> s = statuses.stream().filter(as -> as.getTeam().equals(t)).findFirst();
            if (s.isEmpty()) {
                if (session.getSessionType() == CompetitionSession.SessionType.GROUP) {
                    results.add(TeamFeedback.builder().team(t).build());
                }
            } else {
                if (session.getSessionType() == CompetitionSession.SessionType.GROUP || s.get().isActive()) {
                    TeamFeedback tf = TeamFeedback.builder().team(t).build();
                    TestAttempt ta = s.get().getMostRecentTestAttempt();
                    SubmitAttempt sa = s.get().getMostRecentSubmitAttempt();
                    if (sa != null && sa.getTestAttempt().equals(ta)) {
                        tf.setSubmitted(true);
                        tf.setSuccess(sa.getSuccess() != null && sa.getSuccess());
                        ta = sa.getTestAttempt();
                    }
                    if (ta == null) {
                        results.add(TeamFeedback.builder().team(t).build());
                    } else {
                        ta.getTestCases().forEach(tc -> {
                            tf.getTestResults().put(tc.getName(), tc.getSuccess() != null && tc.getSuccess());
                        });
                        results.add(tf);
                    }
                }
            }
        }
        return results;
    }
}
