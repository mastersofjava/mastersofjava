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

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.TaskControlController.TaskMessage;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.message.model.*;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.service.TestResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class MessageService {

    private static final String DEST_COMPETITION = "/queue/competition";
    private static final String DEST_TESTRESULTS = "/queue/feedbackpage";
    private static final String DEST_CONTROL_FEEDBACK = "/queue/controlfeedback";
    private static final String DEST_START = "/queue/start";
    private static final String DEST_STOP = "/queue/stop";
    private static final String DEST_RANKINGS = "/queue/rankings";

    private SimpMessagingTemplate template;

    public MessageService(SimpMessagingTemplate template) {
        super();
        this.template = template;
    }

    public void sendTestFeedback(Team team, TestResult tr) {
        TeamTestFeedbackMessage msg = TeamTestFeedbackMessage.builder()
                .success(tr.isSuccess())
                .uuid(team.getUuid())
                .test(tr.getTestName())
                .message(tr.getTestOutput() == null ? "" : tr.getTestOutput())
                .build();
        log.info("Sending test assignmentScores: {}", msg);
        template.convertAndSendToUser(team.getName(), DEST_COMPETITION, msg);
        template.convertAndSend(DEST_TESTRESULTS, msg);
    }

    public void sendSubmitFeedback(Team team, SubmitResult submitResult) {
        TeamSubmitFeedbackMessage msg = TeamSubmitFeedbackMessage.builder()
                .score(submitResult.getScore())
                .remainingSubmits(submitResult.getRemainingSubmits())
                .uuid(team.getUuid())
                .team(team.getName())
                .success(submitResult.isSuccess())
                .message("TODO")
                .build();

        log.info("Sending submit assignmentScores: {}", msg);
        template.convertAndSendToUser(msg.getTeam(), DEST_COMPETITION, msg);
        template.convertAndSend(DEST_TESTRESULTS, msg);
        template.convertAndSend(DEST_RANKINGS, "refresh");
    }

    public void sendCompileFeedback(Team team, CompileResult result) {
        TeamCompileFeedbackMessage msg = TeamCompileFeedbackMessage.builder()
                .success(result.isSuccess())
                .team(team.getName())
                .message(result.getCompileOutput() == null ? "" : result.getCompileOutput())
                .build();
        log.info("Sending compile assignmentScores: {}", msg);
        template.convertAndSendToUser(msg.getTeam(), DEST_COMPETITION, msg);
    }

    public void sendStartToTeams(String taskname, String sessionId) {
        template.convertAndSend(DEST_START, taskname);
        template.convertAndSend(DEST_COMPETITION, StartAssignmentMessage.builder().sessionId(sessionId).assignment(taskname).build());
    }

    public void sendStopToTeams(String taskname, String sessionId) {
        template.convertAndSend(DEST_STOP, new TaskMessage(taskname));
        template.convertAndSend(DEST_COMPETITION, StopAssignmentMessage.builder().sessionId(sessionId).assignment(taskname).build());
    }

    public void sendRemainingTime(Long remainingTime, Long totalTime, boolean isPaused, String sessionId) {
        try {
            log.info("Sending time: r={}, t={}, s={}", remainingTime, totalTime, sessionId);
            TimerSyncMessage msg = TimerSyncMessage.builder()
                    .remainingTime(remainingTime)
                    .totalTime(totalTime)
                    .sessionId(sessionId)
                    .isRunning(!isPaused)
                    .build();
            template.convertAndSend(DEST_COMPETITION, msg);
            template.convertAndSend("/queue/time", msg);
        } catch (Exception e) {
            log.warn("Failed to send remaining time.", e);
        }
    }

    public void sendTeamStartedTesting(Team team) {
        log.info("Sending team '{}' started testing.", team.getName());
        template.convertAndSend(DEST_TESTRESULTS, TeamStartedTestingMessage.builder()
                .uuid(team.getUuid())
                .build());
    }

    public void sendStartFail(String name, String cause) {
        log.info("Sending start assignment '{}' failed with cause '{}'", name, cause);
        template.convertAndSend(DEST_CONTROL_FEEDBACK, StartAssignmentFailedMessage.builder()
                .assignment(name).cause(cause).build());
    }

    public void sendComplilingStarted(Team team) {
        log.info("Sending compiling started to team uuid '{}", team.getUuid() );
        template.convertAndSendToUser(team.getName(),DEST_COMPETITION, CompilingStarted.builder().team(team.getUuid()).build());
    }

    public void sendComplilingEnded(Team team, boolean success) {
        log.info("Sending compiling ended to team uuid '{}", team.getUuid() );
        template.convertAndSendToUser(team.getName(),DEST_COMPETITION, CompilingEnded.builder().team(team.getUuid()).success(success).build());
    }

    public void sendTestingStarted(Team team) {
        log.info("Sending testing started to team uuid '{}", team.getUuid() );
        template.convertAndSendToUser(team.getName(),DEST_COMPETITION, TestingStarted.builder().team(team.getUuid()).build());
    }

    public void sendTestingEnded(Team team, boolean success) {
        log.info("Sending testing ended to team uuid '{}", team.getUuid() );
        template.convertAndSendToUser(team.getName(),DEST_COMPETITION, TestingEnded.builder().team(team.getUuid()).success(success).build());
    }

    public void sendSubmitStarted(Team team) {
        log.info("Sending submit started to team uuid '{}", team.getUuid() );
        template.convertAndSendToUser(team.getName(),DEST_COMPETITION, SubmitStarted.builder().team(team.getUuid()).build());
    }

    public void sendSubmitEnded(Team team, boolean success, Long score) {
        log.info("Sending submit ended to team uuid '{}", team.getUuid() );
        template.convertAndSendToUser(team.getName(),DEST_COMPETITION, SubmitEnded.builder().team(team.getUuid()).success(success)
                .score(score).build());
    }
}
