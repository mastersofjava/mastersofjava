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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.*;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestService {

    private final TestCaseRepository testCaseRepository;

    private final TestAttemptRepository testAttemptRepository;

    private final CompileAttemptRepository compileAttemptRepository;

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final JmsTemplate jmsTemplate;

    private final MessageService messageService;

    public void receiveTestResponse(JMSTestResponse testResponse) {
        log.info("Received test attempt response {}", testResponse.getAttempt());
        TestAttempt testAttempt = registerTestResponse(testResponse);
        messageService.sendTestFeedback(testAttempt.getAssignmentStatus().getTeam(), testResponse);
    }

    public TestAttempt registerTestRequest(TestRequest testRequest) {

        messageService.sendTestingStarted(testRequest.getTeam());

        TestAttempt testAttempt = prepareTestAttempt(testRequest);
        // send JMS test request
        jmsTemplate.convertAndSend("test_request", JMSTestRequest.builder()
                .attempt(testAttempt.getUuid())
                .assignment(testRequest.getAssignment().getUuid())
                .sources(testRequest.getSources().entrySet().stream().map(e -> JMSFile.builder()
                        .path(e.getKey().toString())
                        .content(e.getValue())
                        .build()).collect(Collectors.toList()))
                .tests(testAttempt.getTestCases()
                        .stream()
                        .map(tc -> JMSTestCase.builder().testCase(tc.getUuid()).name(tc.getName()).build())
                        .collect(Collectors.toList()))
                .build());

        return testAttempt;
    }

    @Transactional
    public TestAttempt prepareTestAttempt(TestRequest testRequest) {
        AssignmentStatus as = assignmentStatusRepository.findByAssignment_IdAndCompetitionSession_IdAndTeam_Id(
                testRequest.getAssignment().getId(), testRequest.getSession().getId(),
                testRequest.getTeam().getId());

        // register compile attempt
        CompileAttempt compileAttempt = CompileAttempt.builder()
                .assignmentStatus(as)
                .uuid(UUID.randomUUID())
                .dateTimeRegister(Instant.now())
                .build();
        compileAttempt = compileAttemptRepository.save(compileAttempt);

        // register test attempt
        TestAttempt testAttempt = TestAttempt.builder()
                .assignmentStatus(as)
                .uuid(UUID.randomUUID())
                .dateTimeRegister(Instant.now())
                .compileAttempt(compileAttempt)
                .build();

        for (AssignmentFile af : testRequest.getTests()) {
            TestCase tc = TestCase.builder()
                    .uuid(UUID.randomUUID())
                    .testAttempt(testAttempt)
                    .name(af.getName())
                    .dateTimeRegister(Instant.now())
                    .build();
            testAttempt.getTestCases().add(testCaseRepository.save(tc));
        }

        as.getCompileAttempts().add(compileAttempt);
        as.getTestAttempts().add(testAttempt);
        return testAttemptRepository.save(testAttempt);
    }

    @Transactional
    public TestAttempt registerTestResponse(JMSTestResponse testResponse) {
        TestAttempt testAttempt = testAttemptRepository.findByUuid(testResponse.getAttempt());
        testAttempt.setDateTimeStart(testResponse.getStarted());
        testAttempt.setDateTimeEnd(testResponse.getEnded());
        testAttempt.setWorker(testResponse.getWorker());
        testAttempt.setRun(testResponse.getRunId());

        Map<UUID, JMSTestCaseResult> testCaseResults = testResponse.getTestCaseResults().stream()
                .collect(Collectors.toMap(JMSTestCaseResult::getTestCase, tcr -> tcr));

        for (TestCase tc : testAttempt.getTestCases()) {
            if (testCaseResults.containsKey(tc.getUuid())) {
                JMSTestCaseResult tcr = testCaseResults.get(tc.getUuid());
                tc.setTestOutput(tcr.getOutput());
                tc.setDateTimeStart(tcr.getStarted());
                tc.setDateTimeEnd(tcr.getEnded());
                tc.setSuccess(tcr.isSuccess());
                tc.setTimeout(tcr.isTimeout());
                testCaseRepository.save(tc);
            }
        }

        // TODO update compile attempt!

        return testAttemptRepository.save(testAttempt);
    }
}
