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

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.JMSFile;
import nl.moj.common.messages.JMSSubmitResponse;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.common.messages.JMSSubmitRequest;
import nl.moj.common.messages.JMSTestCase;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.model.TestCase;
import nl.moj.server.test.repository.TestCaseRepository;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class SubmitService {

    private final MessageService messageService;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final CompileAttemptRepository compileAttemptRepository;
    private final SubmitAttemptRepository submitAttemptRepository;

    private final TestCaseRepository testCaseRepository;
    private final JmsTemplate jmsTemplate;

    public void receiveSubmitResponse(JMSSubmitResponse submitResponse) {
        log.info("Received submit attempt response {}", submitResponse.getAttempt());
    }

    public SubmitAttempt registerSubmitRequest(SubmitRequest submitRequest) {

        if (submitRequest.isSubmitAllowed()) {
            messageService.sendSubmitStarted(submitRequest.getTeam());
            SubmitAttempt submitAttempt = prepareSubmitAttempt(submitRequest);

            jmsTemplate.convertAndSend("submit_request", JMSSubmitRequest.builder()
                    .attempt(submitAttempt.getUuid())
                    .assignment(submitRequest.getAssignment().getUuid())
                    .sources(submitRequest.getSources().entrySet().stream().map( e -> JMSFile.builder()
                            .path(e.getKey().toString())
                            .content(e.getValue())
                            .build()).collect(Collectors.toList()))
                    .tests(submitAttempt.getTestAttempt().getTestCases()
                            .stream()
                            .map(tc -> JMSTestCase.builder().testCase(tc.getUuid()).name(tc.getName()).build())
                            .collect(Collectors.toList()))
                    .build());

            return submitAttempt;
        }
        log.warn("Submit is not allowed for team '{}' named '{}'", submitRequest.getTeam()
                .getUuid(), submitRequest.getTeam().getName());
        return null;
    }

    @Transactional
    public SubmitAttempt prepareSubmitAttempt(SubmitRequest submitRequest) {
        final AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(submitRequest.getAssignment(), submitRequest.getSession(), submitRequest.getTeam());

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

        for (AssignmentFile af : submitRequest.getTests()) {
            TestCase tc = TestCase.builder()
                    .uuid(UUID.randomUUID())
                    .testAttempt(testAttempt)
                    .name(af.getName())
                    .dateTimeRegister(Instant.now())
                    .build();
            testAttempt.getTestCases().add(testCaseRepository.save(tc));
        }

        SubmitAttempt sa = SubmitAttempt.builder()
                .assignmentStatus(as)
                .dateTimeRegister(Instant.now())
                .assignmentTimeElapsed(submitRequest.getTimeElapsed())
                .uuid(UUID.randomUUID())
                .compileAttempt(compileAttempt)
                .testAttempt(testAttempt)
                .build();

        as.getCompileAttempts().add(compileAttempt);
        as.getTestAttempts().add(testAttempt);
        as.getSubmitAttempts().add(sa);

        return submitAttemptRepository.save(sa);
    }

//    private CompletableFuture<SubmitResult> testInternal(SubmitRequest submitRequest, boolean isSubmit, ActiveAssignment activeAssignment) {
//        //compile
//        return compileInternal(submitRequest, activeAssignment)
//                .thenCompose(submitResult -> {
//                    Assert.isTrue( activeAssignment.getAssignment()!=null, "not ready for test");
//
//                    if (submitResult.isSuccess()) {
//                        // filter selected test cases
//                        var testCases = activeAssignment.getTestFiles().stream()
//                                .filter(t -> submitRequest.getSourceMessage().getTests().contains(t.getUuid().toString()))
//                                .collect(Collectors.toList());
//
//                        if (isSubmit) {
//                            testCases = activeAssignment.getSubmitTestFiles();// contains hidden tests.
//                        }
//                        // run selected testcases
//                        return executionService.test(submitRequest, testCases, activeAssignment).thenApply(r -> {
//
//                            r.getResults().forEach(tr -> messageService.sendTestFeedback(submitRequest.getTeam(), tr));
//
//                            return submitResult.toBuilder()
//                                    .dateTimeEnd(r.getDateTimeEnd())
//                                    .success(r.isSuccess())
//                                    .testResults(r)
//                                    .build();
//                        });
//                    } else {
//                        return CompletableFuture.completedFuture(submitResult);
//                    }
//                });
//    }
//
//    @Transactional
//    public CompletableFuture<SubmitResult> submitInternal(SubmitRequest submitRequest) {
//        final ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment(submitRequest);
//        final AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment
//                .getAssignment(), activeAssignment.getCompetitionSession(), submitRequest.getTeam());
//        boolean isSubmitAllowed = isSubmitAllowedForTeam(as, activeAssignment);
//
//        if (isSubmitAllowed) {
//            messageService.sendSubmitStarted(submitRequest.getTeam());
//            SubmitAttempt sa = SubmitAttempt.builder()
//                    .assignmentStatus(as)
//                    .dateTimeStart(Instant.now())
//                    .assignmentTimeElapsed(activeAssignment.getTimeElapsed())
//                    .uuid(UUID.randomUUID())
//                    .build();
//            as.getSubmitAttempts().add(sa);
//
//            return testInternal(submitRequest, true,activeAssignment).thenApply(sr -> {
//                int remainingSubmits = getRemainingSubmits(as, activeAssignment);
//                try {
//                    if( sr.getCompileResult() != null) {
//                        sa.setCompileAttempt(compileAttemptRepository.findByUuid(sr.getCompileResult()
//                                .getCompileAttemptUuid()));
//                    }
//                    if( sr.getTestResults() != null ) {
//                        sa.setTestAttempt(testAttemptRepository.findByUuid(sr.getTestResults().getTestAttemptUuid()));
//                    }
//                    sa.setSuccess(sr.isSuccess() && activeAssignment.isRunning());
//                    sa.setDateTimeEnd(Instant.now());
//                    submitAttemptRepository.save(sa);
//
//                    remainingSubmits = getRemainingSubmits(as, activeAssignment);
//
//                    if (sr.isSuccess() || remainingSubmits <= 0) {
//                        if (activeAssignment.isRunning() && as.getDateTimeEnd()!=null) {
//                            as.setDateTimeEnd(null);
//                        }
//                        AssignmentStatus scored = scoreService.finalizeScore(as, activeAssignment);
//                        return sr.toBuilder().score(scored.getAssignmentResult().getFinalScore())
//                                .remainingSubmits(0).build();
//                    }
//                } catch (Exception e) {
//                    log.error("Submit failed unexpectedly.", e);
//                }
//
//                return sr.toBuilder().remainingSubmits(remainingSubmits).build();
//            }).thenApply(sr -> {
//                messageService.sendSubmitFeedback(submitRequest.getTeam(), sr);
//                messageService.sendSubmitEnded(submitRequest.getTeam(), sr.isSuccess(), sr.getScore());
//                return sr;
//            });
//        }
//        log.warn("Submit is not allowed for team '{}' named '{}'", submitRequest.getTeam().getUuid(), submitRequest.getTeam().getName());
//        return CompletableFuture.completedFuture(SubmitResult.builder().build());
//    }


}
