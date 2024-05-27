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
package nl.moj.server.submit.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.messages.JMSCompileResponse;
import nl.moj.common.messages.JMSFile;
import nl.moj.common.messages.JMSSubmitRequest;
import nl.moj.common.messages.JMSSubmitResponse;
import nl.moj.common.messages.JMSTestCase;
import nl.moj.common.messages.JMSTestCaseResult;
import nl.moj.common.messages.JMSTestResponse;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionSession.SessionType;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.metrics.MetricsService;
import nl.moj.server.runtime.ScoreService;
import nl.moj.server.runtime.TimersRuntime;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.service.TestRequest;
import nl.moj.server.test.service.TestService;
import nl.moj.server.util.JMSResponseHelper;
import nl.moj.server.util.TransactionHelper;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitService {

    private final MessageService messageService;
    private final TestService testService;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final TeamService teamService;
    private final SubmitAttemptRepository submitAttemptRepository;
    private final ScoreService scoreService;
    private final AssignmentService assignmentService;
    private final JmsTemplate jmsTemplate;
    private final JMSResponseHelper responseHelper;
    private final TaskScheduler taskScheduler;
    private final TransactionHelper trx;
    private final TimersRuntime timersRuntime;
    private final MetricsService metricsService;

    @Transactional
    public void receiveSubmitResponse(JMSSubmitResponse submitResponse) {
        log.info("Received submit attempt response {}", submitResponse.getAttempt());
        SubmitAttempt sa = submitAttemptRepository.findByUuid(submitResponse.getAttempt());
        AssignmentDescriptor ad = assignmentService
                .resolveAssignmentDescriptor(sa.getAssignmentStatus().getAssignment());

        if (sa.getDateTimeEnd() == null) {
            sa = update(sa, submitResponse);
            // score if needed
            if (isSuccess(sa, submitResponse)) {
                sa.setSuccess(true);
                finalizeScore(sa, ad);
            } else {
                sa.setSuccess(false);
                if (sa.getAssignmentStatus().getRemainingSubmitAttempts() <= 0 || assignmentFinished(sa)) {
                    finalizeScore(sa, ad);
                }
            }
            messageService.sendSubmitFeedback(sa);
            metricsService.registerSubmitAttemptMetrics(sa);
        } else {
            log.info("Ignoring response for submit attempt {}, already have a response.", sa.getUuid());
        }
    }

    private void finalizeScore(SubmitAttempt sa, AssignmentDescriptor ad) {
        scoreService.finalizeScore(sa, ad);
        if (sa.getAssignmentStatus() != null && sa.getAssignmentStatus().getCompetitionSession() != null) {
            if (sa.getAssignmentStatus().getCompetitionSession().getSessionType() == SessionType.SINGLE) {
                sa.getAssignmentStatus().setDateTimeEnd(Instant.now());
                timersRuntime.clearTimers(sa.getAssignmentStatus().getTeam());
            }
        }
    }

    private boolean assignmentFinished(SubmitAttempt sa) {
        TeamAssignmentStatus tas = sa.getAssignmentStatus();
        Optional<AssignmentStatus> as = assignmentStatusRepository
                .findByCompetitionSessionAndAssignment(tas.getCompetitionSession(), tas.getAssignment());
        return as.map(AssignmentStatus::getDateTimeEnd).orElse(null) != null;
    }

    private boolean hasNoPendingSubmits(TeamAssignmentStatus teamAssignmentStatus) {
        return submitAttemptRepository.countPending(teamAssignmentStatus) == 0;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public SubmitAttempt update(SubmitAttempt sa, JMSSubmitResponse sr) {

        if (sa == null) {
            return null;
        }
        if (sr == null) {
            return sa;
        }

        sa.setDateTimeStart(sr.getStarted());
        sa.setDateTimeEnd(sr.getEnded());
        sa.setAborted(sr.isAborted());
        sa.setReason(sr.getReason());
        sa.setWorker(sr.getWorker());
        sa.setTrace(sr.getTraceId());
        sa.setSuccess(isSuccess(sa, sr));
        testService.update(sa.getTestAttempt(), sr.getTestResponse());
        return submitAttemptRepository.save(sa);
    }

    private boolean isSuccess(SubmitAttempt sa, JMSSubmitResponse sr) {
        if (sr != null) {
            return !sr.isAborted() && testsSucceeded(sa, sr.getTestResponse());
        }
        return false;
    }

    private boolean testsSucceeded(SubmitAttempt sa, JMSTestResponse tr) {
        if (tr != null) {
            if (compileSucceeded(tr.getCompileResponse())) {
                List<TestCase> testCases = Optional.ofNullable(sa.getTestAttempt().getTestCases())
                        .orElse(Collections.emptyList());
                List<JMSTestCaseResult> testCaseResults = Optional.ofNullable(tr.getTestCaseResults())
                        .orElse(Collections.emptyList());
                // TODO Maybe also check if testCases.size() == assignment.tests
                // check if we got the expected amount of testcases
                if (!testCases.isEmpty() && !testCaseResults.isEmpty() && testCases.size() == testCaseResults.size()) {
                    // match all test cases with test case results
                    return testCases.stream().allMatch(
                            tc -> testCaseResults.stream().filter(tcr -> tcr.getTestCase().equals(tc.getUuid()))
                                    .map(JMSTestCaseResult::isSuccess).findFirst().orElse(false));
                }
            }
        }
        return false;
    }

    private boolean compileSucceeded(JMSCompileResponse cr) {
        if (cr != null) {
            return cr.isSuccess();
        }
        return false;
    }

    @Transactional
    @NewSpan
    public SubmitAttempt registerSubmitAttempt(SubmitRequest submitRequest) throws SubmitAttemptRegisterException {
        log.info("Registering submit attempt for assignment {} by team {}.", submitRequest.getAssignment().getUuid(),
                submitRequest.getTeam().getUuid());
        final TeamAssignmentStatus tas = teamAssignmentStatusRepository
                .findByAssignmentAndCompetitionSessionAndTeam(submitRequest.getAssignment(), submitRequest.getSession(),
                        submitRequest.getTeam())
                .orElseThrow(() -> new IllegalStateException(
                        "Submit request received for assignment " + submitRequest.getAssignment().getUuid()
                                + " that was never joined by team " + submitRequest.getTeam().getUuid() + "."));
        final AssignmentStatus as = assignmentStatusRepository
                .findByCompetitionSessionAndAssignment(tas.getCompetitionSession(), tas.getAssignment())
                .orElseThrow(() -> new IllegalStateException("Submit request received for assignment "
                        + tas.getAssignment().getUuid() + " that was never started."));

        if (hasNoPendingSubmits(tas)) {
            Instant registered = Instant.now();

            long secondsRemaining = tas.getCompetitionSession().getSessionType() == SessionType.GROUP
                    ? timersRuntime.getGroupSecondsRemaining(registered)
                    : timersRuntime.getTeamSecondsRemaining(submitRequest.getTeam(), registered);

            boolean registeredOnTime = tas.getCompetitionSession().getSessionType() == SessionType.GROUP
                    ? timersRuntime.isGroupRegisteredBeforeEnding(as, registered)
                    : timersRuntime.isTeamRegisteredBeforeEnding(submitRequest.getTeam(), registered);

            int remainingAttempts = tas.getRemainingSubmitAttempts();
            if (remainingAttempts > 0 && registeredOnTime) {
                log.debug("Has remaining attempts {} and is registered on time ({}s remaining)", remainingAttempts,
                        secondsRemaining);
                try {
                    // save the team progress
                    teamService.updateAssignment(submitRequest.getTeam().getUuid(), submitRequest.getSession()
                            .getUuid(),
                            submitRequest.getAssignment().getUuid(), submitRequest.getSources());

                    SubmitAttempt submitAttempt = prepareSubmitAttempt(submitRequest, registered,
                            Duration.ofSeconds(secondsRemaining));

                    jmsTemplate.convertAndSend("operation_request",
                            JMSSubmitRequest.builder().attempt(submitAttempt.getUuid())
                                    .assignment(submitRequest.getAssignment().getUuid())
                                    .sources(submitRequest.getSources().entrySet().stream()
                                            .map(e -> JMSFile.builder()
                                                    .type(JMSFile.Type.SOURCE)
                                                    .path(e.getKey().toString())
                                                    .content(e.getValue())
                                                    .build())
                                            .collect(Collectors.toList()))
                                    .tests(submitAttempt.getTestAttempt().getTestCases().stream()
                                            .map(tc -> JMSTestCase.builder()
                                                    .testCase(tc.getUuid())
                                                    .name(tc.getName())
                                                    .build())
                                            .collect(Collectors.toList()))
                                    .build());

                    // schedule controller abort
                    scheduleAbort(submitAttempt);

                    messageService.sendSubmitStarted(submitRequest.getTeam());

                    log.info("Submit attempt {} for assignment {} by team {} registered.", submitAttempt.getUuid(),
                            submitRequest.getAssignment()
                                    .getUuid(),
                            submitRequest.getTeam().getUuid());

                    return submitAttempt;
                } catch (IOException e) {
                    messageService.sendSubmitUnprocessable(submitRequest.getTeam());
                    throw new SubmitAttemptRegisterException(String.format(
                            "Failed to register submit attempt for assignment %s by team %s.", submitRequest.getAssignment()
                                    .getUuid(),
                            submitRequest.getTeam().getUuid()), e);
                }
            }
            log.warn("Submit is not allowed for team '{}' named '{}'", submitRequest.getTeam()
                    .getUuid(), submitRequest.getTeam().getName());

            messageService.sendSubmitRejected(submitRequest.getTeam(),
                    remainingAttempts > 0 ? "Submit received after assignment ended." : "No more submit attempts left.");
            // send submit rejected
            throw new SubmitAttemptRegisterException(
                    remainingAttempts > 0 ? "Submit received after assignment ended." : "No more submit attempts left.");
        }

        // maybe send rejected message with another submit is still pending.

        throw new SubmitAttemptRegisterException("Submit not allowed, submit still pending.");
    }

    private SubmitAttempt prepareSubmitAttempt(SubmitRequest submitRequest, Instant registered,
            Duration timeRemaining) {
        final TeamAssignmentStatus as = teamAssignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(
                submitRequest.getAssignment(), submitRequest.getSession(), submitRequest.getTeam()).orElseThrow();

        TestAttempt testAttempt = testService.prepareTestAttempt(TestRequest.builder().tests(submitRequest.getTests())
                .session(submitRequest.getSession()).sources(submitRequest.getSources())
                .assignment(submitRequest.getAssignment()).team(submitRequest.getTeam()).build());

        SubmitAttempt sa = SubmitAttempt.builder().assignmentStatus(as).dateTimeRegister(registered)
                .assignmentTimeRemaining(timeRemaining).uuid(UUID.randomUUID()).testAttempt(testAttempt).build();

        as.getSubmitAttempts().add(sa);
        return submitAttemptRepository.save(sa);
    }

    private void scheduleAbort(SubmitAttempt submitAttempt) {
        Duration timeout = assignmentService
                .resolveSubmitAbortTimout(submitAttempt.getAssignmentStatus().getAssignment());
        taskScheduler.schedule(() -> {
            trx.required(() -> {
                SubmitAttempt sa = submitAttemptRepository.findByUuid(submitAttempt.getUuid());
                if (sa != null && sa.getDateTimeEnd() == null && sa.getAssignmentStatus().getDateTimeEnd() == null) {
                    log.info("Aborting submit attempt {}, response took too long.", sa.getUuid());
                    receiveSubmitResponse(responseHelper.abortResponse(sa));
                }
            });
        }, Instant.now().plus(timeout));
    }
}
