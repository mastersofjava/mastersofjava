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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.ScoreService;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.repository.TestAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@AllArgsConstructor
@Slf4j
public class SubmitService {

    private final CompetitionRuntime competitionRuntime;
    private final ExecutionService executionService;
    private final ScoreService scoreService;
    private final MessageService messageService;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final CompileAttemptRepository compileAttemptRepository;
    private final SubmitAttemptRepository submitAttemptRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;
    private final CompetitionSessionRepository competitionSessionRepository;

    public CompletableFuture<SubmitResult> compile(Team team, SourceMessage message) {
        final Assignment assignment = determineAssignment(team, message);
        messageService.sendComplilingStarted(team);
        return compileInternal(team, message, assignment).thenApply(r -> {
            log.info("DONE with compile request team {}, cleaning workspace afterwards " , team.getName());
            messageService.sendComplilingEnded(team, r.isSuccess());
            return r;
        });
    }

    private CompletableFuture<SubmitResult> compileInternal(Team team, SourceMessage message, Assignment assignment) {
        return executionService.compile(team, message, getActiveAssignment(team, message))
                .thenApply(r -> {
                    log.info("DONE with compile execution team {}", team.getName());
                    messageService.sendCompileFeedback(team, r);

                    return SubmitResult.builder()
                            .team(team.getUuid())
                            .dateTimeStart(r.getDateTimeStart())
                            .dateTimeEnd(r.getDateTimeEnd())
                            .success(r.isSuccess())
                            .compileResult(r)
                            .build();
                });
    }

    public CompletableFuture<SubmitResult> test(Team team, SourceMessage message) {
        return test(team, message, false);
    }

    public CompletableFuture<SubmitResult> test(Team team, SourceMessage message, boolean submit) {
        messageService.sendTestingStarted(team);
        final Assignment assignment = determineAssignment(team, message);
        return testInternal(team, message, submit, assignment).thenApply(r -> {
            log.info("DONE with test request team {}, cleaning workspace afterwards " , team.getName());
            messageService.sendTestingEnded(team, r.isSuccess());
            return r;
        });
    }
    private ActiveAssignment getActiveAssignment(Team team, SourceMessage message) {
        UUID uuid = UUID.fromString(message.getUuid());
        Competition competition = competitionRuntime.selectCompetitionByUUID(uuid);
        if (!team.getRole().equals(Role.ADMIN)) {
            return competitionRuntime.selectCompetitionRuntimeForGameStart(competition).getActiveAssignment();
        }
        ActiveAssignment activeAssignment = null;
        if (competition!=null) {
            activeAssignment = competitionRuntime.selectCompetitionRuntimeForGameStart(competition).getActiveAssignment();
        }
        if (activeAssignment!=null && message.getAssignmentName().equals(activeAssignment.getAssignment().getName())) {
            return activeAssignment;
        }
        CompetitionSession competitionSession = competitionRuntime.getCompetitionSession();
        if (!message.getUuid().equals(competitionSession.getUuid())) {
            competitionSession = competitionSessionRepository.findByUuid(uuid);
        }
        Assignment assignment = assignmentRepository.findByName(message.getAssignmentName() );
        List<AssignmentFile> fileList   = assignmentService.getAssignmentFiles(assignment);
        AssignmentDescriptor assignmentDescriptor =assignmentService.getAssignmentDescriptor(assignment);
        activeAssignment  = ActiveAssignment.builder().competitionSession(competitionSession).assignment(assignment).assignmentDescriptor(assignmentDescriptor).assignmentFiles(fileList).build();

        return activeAssignment;
    }
    private Assignment determineAssignment(Team team, SourceMessage message) {
        ActiveAssignment activeAssignment = getActiveAssignment(team, message);
        if (!team.getRole().equals(Role.ADMIN)) {
            return activeAssignment.getAssignment();
        }
        return assignmentRepository.findByName(message.getAssignmentName() );
    }

    private CompletableFuture<SubmitResult> testInternal(Team team, SourceMessage message, boolean submit, Assignment assignment) {
        log.info("testInternal start with ( assignent {}, submit {}) ", message.getAssignmentName() , submit);

        //compile
        return compileInternal(team, message, assignment)
                .thenCompose(submitResult -> {
                    log.info("testInternal.after compile ( assignent {}, compile {}) ", message.getAssignmentName(),  submitResult.isSuccess() );
                    ActiveAssignment activeAssignment = getActiveAssignment(team, message);

                    Assert.isTrue( activeAssignment.getAssignment()!=null, "not ready for test");

                    if (submitResult.isSuccess()) {
                        // filter selected test cases
                        List<AssignmentFile> testCases = activeAssignment.getTestFiles().stream()
                                .filter(t -> message.getTests().contains(t.getUuid().toString()))
                                .collect(Collectors.toList());

                        if (submit || team.getRole().equals(Role.ADMIN)) {
                            testCases = activeAssignment.getSubmitTestFiles();
                        }
                        log.info("testCases (size {}, compile {}, assignment {}, session {}) " , testCases.size(), submitResult.isSuccess(), activeAssignment.getAssignment().getName(), activeAssignment.getCompetitionSession().getUuid());

                        // run selected testcases
                        return executionService.test(team, testCases, activeAssignment).thenApply(r -> {

                            r.getResults().forEach(tr -> messageService.sendTestFeedback(team, tr));

                            return submitResult.toBuilder()
                                    .dateTimeEnd(r.getDateTimeEnd())
                                    .success(r.isSuccess())
                                    .testResults(r)
                                    .build();
                        });
                    } else {
                        return CompletableFuture.completedFuture(submitResult);
                    }
                });
    }

    @Transactional
    public CompletableFuture<SubmitResult> submit(Team team, SourceMessage message) {
        final ActiveAssignment activeAssignment = getActiveAssignment(team, message );
        final AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment
                .getAssignment(), activeAssignment.getCompetitionSession(), team);

        if (isSubmitAllowedForTeam(as)) {
            messageService.sendSubmitStarted(team);
            SubmitAttempt sa = SubmitAttempt.builder()
                    .assignmentStatus(as)
                    .dateTimeStart(Instant.now())
                    .assignmentTimeElapsed(activeAssignment.getTimeElapsed())
                    .uuid(UUID.randomUUID())
                    .build();
            as.getSubmitAttempts().add(sa);

            return testInternal(team, message, true,activeAssignment
                    .getAssignment()).thenApply(sr -> {
                int remainingSubmits = getRemainingSubmits(as);
                try {
                    if( sr.getCompileResult() != null) {
                        sa.setCompileAttempt(compileAttemptRepository.findByUuid(sr.getCompileResult()
                                .getCompileAttemptUuid()));
                    }
                    if( sr.getTestResults() != null ) {
                        sa.setTestAttempt(testAttemptRepository.findByUuid(sr.getTestResults().getTestAttemptUuid()));
                    }
                    sa.setSuccess(sr.isSuccess());
                    sa.setDateTimeEnd(Instant.now());
                    submitAttemptRepository.save(sa);

                    remainingSubmits = getRemainingSubmits(as);

                    if (sr.isSuccess() || remainingSubmits <= 0) {
                        AssignmentStatus scored = scoreService.finalizeScore(as, activeAssignment);
                        return sr.toBuilder().score(scored.getAssignmentResult().getFinalScore())
                                .remainingSubmits(0).build();
                    }
                } catch (Exception e) {
                    log.error("Submit failed unexpectedly.", e);
                }

                return sr.toBuilder().remainingSubmits(remainingSubmits).build();
            }).thenApply(sr -> {
                messageService.sendSubmitFeedback(team, sr);
                messageService.sendSubmitEnded(team, sr.isSuccess(), sr.getScore());
                return sr;
            });
        }
        return CompletableFuture.completedFuture(SubmitResult.builder().build());
    }

    private boolean isSubmitAllowedForTeam(AssignmentStatus as) {
        return getRemainingSubmits(as) > 0;
    }

    private int getRemainingSubmits(AssignmentStatus as) {
        Competition competition = competitionRuntime.selectCompetitionByUUID(as.getCompetitionSession().getUuid());
        ActiveAssignment activeAssignment = competitionRuntime.selectCompetitionRuntimeForGameStart(competition).getActiveAssignment();
        int maxSubmits = activeAssignment.getAssignmentDescriptor().getScoringRules().getMaximumResubmits() + 1;
        return maxSubmits - as.getSubmitAttempts().size();
    }
}
