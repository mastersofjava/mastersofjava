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
package nl.moj.server.message.service;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.message.model.*;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import javax.transaction.Transactional;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class MessageService {

    private static final String DEST_COMPETITION = "/queue/session";
    private static final String DEST_TESTRESULTS = "/queue/feedbackpage";
    private static final String DEST_CONTROL_FEEDBACK = "/queue/controlfeedback";
    private static final String DEST_START = "/queue/start";
    private static final String DEST_STOP = "/queue/stop";
    private static final String DEST_RANKINGS = "/queue/rankings";

    private final CompetitionSessionRepository competitionSessionRepository;
    private final UserService userService;
    private final SimpMessagingTemplate template;
    private final Tracer tracer;

    @Autowired
    public MessageService(SimpMessagingTemplate template, CompetitionSessionRepository competitionSessionRepository, UserService userService, Tracer tracer) {
        super();
        this.template = template;
        this.competitionSessionRepository = competitionSessionRepository;
        this.userService = userService;
        this.tracer = tracer;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendTestFeedback(TestAttempt ta) {
        if (ta != null) {
            Team team = ta.getAssignmentStatus().getTeam();
            sendCompileFeedback(ta.getCompileAttempt());
            ta.getTestCases().forEach(tc -> {
                sendTestFeedback(team, tc);
            });
        }
    }

    private void sendTestFeedback(Team team, TestCase tc) {
        TeamTestFeedbackMessage msg = TeamTestFeedbackMessage.builder()
                .success(tc.getSuccess())
                .uuid(team.getUuid())
                .testId(tc.getUuid())
                .test(tc.getName())
                .message(tc.getTestOutput())
                .rejected(false)
                .traceId(getTraceId())
                .build();
        log.info("Sending test feedback: {}", msg);
        sendToActiveUsers(team, msg);
        template.convertAndSend(DEST_TESTRESULTS, msg);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendSubmitFeedback(SubmitAttempt sa) {
        TeamAssignmentStatus as = sa.getAssignmentStatus();
        sendSubmitFeedback(sa, as);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendSubmitFeedback(TeamAssignmentStatus as) {
        SubmitAttempt sa = as.getMostRecentSubmitAttempt();
        sendSubmitFeedback(sa, as);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendSubmitRejected(Team team, String reason) {
        TeamSubmitFeedbackMessage msg = TeamSubmitFeedbackMessage.builder()
                .score(0L)
                .remainingSubmits(0)
                .uuid(team.getUuid())
                .team(team.getName())
                .success(false)
                .completed(false)
                .rejected(true)
                .traceId(getTraceId())
                .message(reason)
                .build();
        sendToActiveUsers(team, msg);
    }

    private void sendSubmitFeedback(SubmitAttempt sa, TeamAssignmentStatus as) {
        AssignmentResult ar = as.getAssignmentResult();
        Team team = as.getTeam();
        TeamSubmitFeedbackMessage msg = TeamSubmitFeedbackMessage.builder()
                .score(ar != null ? ar.getFinalScore() : 0L)
                .remainingSubmits(as.getAssignment().getAllowedSubmits() - as.getSubmitAttempts().size())
                .uuid(team.getUuid())
                .team(team.getName())
                .success(sa != null && sa.getSuccess() != null && sa.getSuccess())
                .completed(as.getDateTimeCompleted() != null)
                .rejected(false)
                .traceId(getTraceId())
                .message("TODO")
                .build();

        log.info("Sending submit feedback: {}", msg);
        if (sa != null) {
            sendTestFeedback(sa.getTestAttempt());
        }
        sendToActiveUsers(team, msg);
        template.convertAndSend(DEST_TESTRESULTS, msg);
        template.convertAndSend(DEST_RANKINGS, "refresh");
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendCompileFeedback(CompileAttempt ca) {//Team team, JMSCompileResponse compileResponse) {
        if (ca != null) {
            Team team = ca.getAssignmentStatus().getTeam();
            TeamCompileFeedbackMessage msg = TeamCompileFeedbackMessage.builder()
                    .success(ca.getSuccess())
                    .team(team.getName())
                    .message(ca.getCompilerOutput() == null ? "" : ca.getCompilerOutput())
                    .rejected(false)
                    .traceId(getTraceId())
                    .build();
            log.info("Sending compile feedback: {}", msg);
            sendToActiveUsers(team, msg);
        }
    }

    public void sendStartToTeams(String taskname, String sessionId) {
        log.info("Sending start: t={}, s={}", taskname, sessionId);
        template.convertAndSend(DEST_START, taskname);
        template.convertAndSend(DEST_COMPETITION, StartAssignmentMessage.builder()
                .sessionId(sessionId)
                .assignment(taskname)
                .build());
    }

    public void sendStopToTeams(String taskname, String sessionId) {
        log.info("Sending stop: t={}, s={}", taskname, sessionId);
        template.convertAndSend(DEST_STOP, Map.of("taskName", taskname));
        template.convertAndSend(DEST_COMPETITION, StopAssignmentMessage.builder()
                .sessionId(sessionId)
                .assignment(taskname)
                .build());
    }

    public void sendRemainingTime(Duration remainingTime, Duration totalTime, UUID session) {
        try {
            log.info("Sending time: r={}, t={}, s={}", remainingTime, totalTime, session.toString());
            TimerSyncMessage msg = TimerSyncMessage.builder()
                    .remainingTime(remainingTime.toSeconds())
                    .totalTime(totalTime.toSeconds())
                    .sessionId(session.toString())
                    .isRunning(true)
                    .build();
            template.convertAndSend(DEST_COMPETITION, msg);
            template.convertAndSend("/queue/time", msg);

        } catch (Exception e) {
            log.warn("Failed to send remaining time.", e);
        }
    }

    public void sendStartFail(String name, String cause) {
        log.info("Sending start assignment '{}' failed with cause '{}'", name, cause);
        template.convertAndSend(DEST_CONTROL_FEEDBACK, StartAssignmentFailedMessage.builder()
                .assignment(name).cause(cause).build());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendCompilingStarted(Team team) {
        log.info("Sending compiling started for team '{}' ", team.getUuid());
        sendToActiveUsers(team, CompilingStarted.builder().team(team.getUuid()).build());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendCompilingEnded(Team team, boolean success) {
        log.info("Sending compiling ended for team uuid '{}'", team.getUuid());
        sendToActiveUsers(team, CompilingEnded.builder().team(team.getUuid()).success(success).build());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendTestingStarted(Team team) {
        log.info("Sending testing started for team uuid '{}'", team.getUuid());
        sendToActiveUsers(team, TestingStarted.builder().team(team.getUuid()).build());
        template.convertAndSend(DEST_TESTRESULTS, TeamStartedTestingMessage.builder()
                .uuid(team.getUuid())
                .build());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendTestingEnded(Team team, boolean success) {
        log.info("Sending testing ended for team uuid '{}'", team.getUuid());
        sendToActiveUsers(team, TestingEnded.builder().team(team.getUuid()).success(success).build());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendSubmitStarted(Team team) {
        log.info("Sending submit started for team uuid '{}'", team.getUuid());
        sendToActiveUsers(team, SubmitStarted.builder().team(team.getUuid()).build());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void sendSubmitEnded(Team team, boolean success, Long score) {
        log.info("Sending submit ended for team uuid '{}'", team.getUuid());
        sendToActiveUsers(team, SubmitEnded.builder().team(team.getUuid()).success(success)
                .score(score).build());
    }

    private void sendToActiveUsers(Team team, Object payload) {
        getActiveUsers(team).forEach(u -> {
            template.convertAndSendToUser(u.getName(), DEST_COMPETITION, payload);
        });
    }

    private Set<User> getActiveUsers(Team t) {
        return userService.getActiveUsers().stream().filter(u -> t.getUsers().contains(u)).collect(Collectors.toSet());
    }

    private String getTraceId() {
        Span s = tracer.currentSpan();
        if (s != null && s.context() != null) {
            return s.context().traceId();
        }
        return null;
    }
}
