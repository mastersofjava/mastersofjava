package nl.moj.server.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.feedback.model.TeamFeedback;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.model.TestAttempt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final TeamRepository teamRepository;

    public List<TeamFeedback> getAssignmentFeedback(Assignment assignment, CompetitionSession session) {
        List<Team> allTeams = teamRepository.findAll();
        if( assignment != null && session != null ) {
            List<AssignmentStatus> statuses = assignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment,session);
            List<TeamFeedback> results = new ArrayList<>();
            for( Team t : allTeams ) {
                Optional<AssignmentStatus> s = statuses.stream().filter(as -> as.getTeam().equals(t)).findFirst();
                if( s.isEmpty() ) {
                    results.add( TeamFeedback.builder().team(t).build());
                } else {
                    TeamFeedback tf = TeamFeedback.builder().team(t).build();
                    TestAttempt ta = s.get().getMostRecentTestAttempt();
                    SubmitAttempt sa = s.get().getMostRecentSubmitAttempt();
                    if( sa != null && sa.getTestAttempt().equals(ta)) {
                        tf.setSubmitted(true);
                        tf.setSuccess(sa.isSuccess());
                        ta = sa.getTestAttempt();
                    }
                    if( ta == null ) {
                        results.add(TeamFeedback.builder().team(t).build());
                    } else {
                        ta.getTestCases().forEach(tc -> {
                            tf.getTestResults().put(tc.getName(), tc.getSuccess() != null && tc.getSuccess());
                        });
                        results.add(tf);
                    }
                }
            }
            return results;
        } else {
            return allTeams.stream().map( t -> TeamFeedback.builder().team(t).build()).collect(Collectors.toList());
        }
    }
}
