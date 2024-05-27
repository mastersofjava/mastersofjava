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
package nl.moj.server.test.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.*;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.service.CompileRequest;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.metrics.MetricsService;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import nl.moj.server.util.JMSResponseHelper;
import nl.moj.server.util.TransactionHelper;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestService {

    private final CompileService compileService;
    private final TestCaseRepository testCaseRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final TeamService teamService;
    private final JmsTemplate jmsTemplate;
    private final MessageService messageService;
    private final JMSResponseHelper responseHelper;
    private final AssignmentService assignmentService;
    private final TaskScheduler taskScheduler;
    private final TransactionHelper trx;
    private final MetricsService metricsService;

    @Transactional
    public void receiveTestResponse(JMSTestResponse testResponse) {
        log.info("Received test attempt response {}", testResponse.getAttempt());
        TestAttempt testAttempt = testAttemptRepository.findByUuid(testResponse.getAttempt());

        if (testAttempt.getDateTimeEnd() == null) {
            testAttempt = update(testAttempt, testResponse);
            if (isMostRecent(testAttempt)) {
                messageService.sendTestFeedback(testAttempt);
            } else {
                log.info("Ignoring test feedback for test attempt {}, already a newer test attempt pending.",
                        testAttempt.getUuid());
            }
            metricsService.registerTestAttemptMetrics(testAttempt);
        } else {
            log.info("Ignoring response for test attempt {}, already have a response.", testAttempt.getUuid());
        }
    }

    private boolean isMostRecent(TestAttempt testAttempt) {
        return testAttemptRepository.countNewerAttempts(testAttempt.getAssignmentStatus(),
                testAttempt.getDateTimeRegister()) == 0;
    }

    @Transactional
    @NewSpan
    public TestAttempt registerTestAttempt(TestRequest testRequest) throws TestAttemptRegisterException {

        log.info("Registering test attempt for assignment {} by team {}.", testRequest.getAssignment()
                .getUuid(), testRequest.getTeam().getUuid());

        try {
            // save the team progress
            teamService.updateAssignment(testRequest.getTeam().getUuid(), testRequest.getSession().getUuid(),
                    testRequest.getAssignment().getUuid(), testRequest.getSources());

            TestAttempt testAttempt = prepareTestAttempt(testRequest);
            // send JMS test request
            jmsTemplate.convertAndSend("operation_request", JMSTestRequest.builder()
                    .attempt(testAttempt.getUuid())
                    .assignment(testRequest.getAssignment().getUuid())
                    .sources(testRequest.getSources().entrySet().stream().map(e -> JMSFile.builder()
                            .type(JMSFile.Type.SOURCE)
                            .path(e.getKey().toString())
                            .content(e.getValue())
                            .build()).collect(Collectors.toList()))
                    .tests(testAttempt.getTestCases()
                            .stream()
                            .map(tc -> JMSTestCase.builder().testCase(tc.getUuid()).name(tc.getName()).build())
                            .collect(Collectors.toList()))
                    .build());

            // schedule controller abort
            scheduleAbort(testAttempt);

            messageService.sendTestingStarted(testRequest.getTeam());

            log.info("Test attempt {} for assignment {} by team {} registered.", testAttempt.getUuid(),
                    testRequest.getAssignment()
                            .getUuid(),
                    testRequest.getTeam().getUuid());
            return testAttempt;

        } catch (IOException e) {
            messageService.sendTestUnprocessable(testRequest.getTeam());
            throw new TestAttemptRegisterException(
                    String.format("Unable to register test attempt for assignment %s by team %s.", testRequest.getAssignment()
                            .getUuid(), testRequest.getTeam().getUuid()),
                    e);
        }
    }

    @Transactional
    public TestAttempt prepareTestAttempt(TestRequest testRequest) {
        TeamAssignmentStatus as = teamAssignmentStatusRepository.findByAssignment_IdAndCompetitionSession_IdAndTeam_Id(
                testRequest.getAssignment().getId(), testRequest.getSession().getId(),
                testRequest.getTeam().getId());

        CompileAttempt compileAttempt = compileService.prepareCompileAttempt(
                CompileRequest.builder()
                        .assignment(testRequest.getAssignment())
                        .session(testRequest.getSession())
                        .sources(testRequest.getSources())
                        .team(testRequest.getTeam())
                        .build());

        TestAttempt testAttempt = TestAttempt.builder()
                .assignmentStatus(as)
                .uuid(UUID.randomUUID())
                .dateTimeRegister(Instant.now())
                .compileAttempt(compileAttempt)
                .build();
        testAttempt = testAttemptRepository.save(testAttempt);

        for (AssignmentFile af : testRequest.getTests()) {
            TestCase tc = TestCase.builder()
                    .uuid(UUID.randomUUID())
                    .testAttempt(testAttempt)
                    .name(af.getName())
                    .dateTimeRegister(Instant.now())
                    .build();
            testAttempt.getTestCases().add(testCaseRepository.save(tc));
        }

        as.getTestAttempts().add(testAttempt);
        return testAttempt;
    }

    @Transactional
    public TestAttempt registerTestResponse(JMSTestResponse testResponse) {
        TestAttempt testAttempt = testAttemptRepository.findByUuid(testResponse.getAttempt());

        if (testAttempt.getDateTimeEnd() != null) {
            log.info("Ignoring response for test attempt {}, already have a response.", testAttempt.getUuid());
            return testAttempt;
        }

        return update(testAttempt, testResponse);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public TestAttempt update(TestAttempt testAttempt, JMSTestResponse testResponse) {

        if (testAttempt == null) {
            return null;
        }
        if (testResponse == null) {
            return testAttempt;
        }

        testAttempt.setDateTimeStart(testResponse.getStarted());
        testAttempt.setDateTimeEnd(testResponse.getEnded());
        testAttempt.setWorker(testResponse.getWorker());
        testAttempt.setTrace(testResponse.getTraceId());
        testAttempt.setAborted(testResponse.isAborted());
        testAttempt.setReason(testResponse.getReason());

        Map<UUID, JMSTestCaseResult> testCaseResults = testResponse.getTestCaseResults().stream()
                .collect(Collectors.toMap(JMSTestCaseResult::getTestCase, tcr -> tcr));

        if (testAttempt.getTestCases() != null) {
            for (TestCase tc : testAttempt.getTestCases()) {
                if (testCaseResults.containsKey(tc.getUuid())) {
                    JMSTestCaseResult tcr = testCaseResults.get(tc.getUuid());
                    tc.setWorker(tcr.getWorker());
                    tc.setTrace(tcr.getTraceId());
                    tc.setTestOutput(tcr.getOutput());
                    tc.setDateTimeStart(tcr.getStarted());
                    tc.setDateTimeEnd(tcr.getEnded());
                    tc.setSuccess(tcr.isSuccess());
                    tc.setTimeout(tcr.isTimeout());
                    tc.setAborted(tcr.isAborted());
                    tc.setReason(tcr.getReason());
                    testCaseRepository.save(tc);
                }
            }
        }
        compileService.update(testAttempt.getCompileAttempt(), testResponse.getCompileResponse());
        return testAttemptRepository.save(testAttempt);
    }

    private void scheduleAbort(TestAttempt testAttempt) {
        Duration timeout = assignmentService.resolveTestAbortTimout(testAttempt.getAssignmentStatus().getAssignment(),
                testAttempt.getTestCases().size());
        taskScheduler.schedule(() -> {
            trx.required(() -> {
                TestAttempt ta = testAttemptRepository.findByUuid(testAttempt.getUuid());
                if (ta != null && ta.getDateTimeEnd() == null && ta.getAssignmentStatus().getDateTimeEnd() == null) {
                    log.info("Aborting test attempt {}, response took too long.", ta.getUuid());
                    receiveTestResponse(responseHelper.abortResponse(ta));
                }
            });
        }, Instant.now().plus(timeout));
    }
}
