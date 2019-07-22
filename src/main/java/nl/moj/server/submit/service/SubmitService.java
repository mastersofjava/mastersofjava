package nl.moj.server.submit.service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.compiler.service.CompileResult;
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
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.service.TestResults;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class SubmitService {

    private final CompetitionRuntime competition;
    private final ExecutionService executionService;
    private final ScoreService scoreService;
    private final MessageService messageService;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final CompileAttemptRepository compileAttemptRepository;
    private final SubmitAttemptRepository submitAttemptRepository;

    public CompletableFuture<SubmitResult> compile(Team team, SourceMessage message) {
        return compileInternal(team, message).thenApply(r ->
                SubmitResult.builder()
                        .team(team.getUuid())
                        .dateTimeStart(r.getDateTimeStart())
                        .success(r.isSuccess())
                        .compileResult(r)
                        .build());
    }

    private CompletableFuture<CompileResult> compileInternal(Team team, SourceMessage message) {
        return executionService.compile(team, message)
                .thenApply(r -> {
                    messageService.sendCompileFeedback(team, r);
                    return r;
                });
    }

    public CompletableFuture<SubmitResult> test(Team team, SourceMessage message) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        //compile
        return compile(team, message)
                .thenCompose(sr -> {
                    if (sr.isSuccess()) {

                        // filter selected test cases
                        var testCases = activeAssignment.getTestFiles().stream()
                                .filter(t -> message.getTests().contains(t.getUuid().toString()))
                                .collect(Collectors.toList());

                        // run selected testcases
                        return testInternal(team, testCases)
                                .thenApply(tr -> sr.toBuilder()
                                        .dateTimeEnd(tr.getDateTimeEnd())
                                        .success(tr.isSuccess())
                                        .testResults(tr)
                                        .build());
                    } else {
                        return CompletableFuture.completedFuture(sr);
                    }
                });
    }

    private CompletableFuture<TestResults> testInternal(Team team, List<AssignmentFile> tests) {
        return executionService.test(team, tests).thenApply(r -> {
            r.getResults().forEach(tr -> messageService.sendTestFeedback(team, tr));
            return r;
        });
    }


    @Transactional
    public CompletableFuture<SubmitResult> submit(Team team, SourceMessage message) {
        final ActiveAssignment activeAssignment = competition.getActiveAssignment();
        final AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment
                .getAssignment(), activeAssignment.getCompetitionSession(), team);

        if (isSubmitAllowedForTeam(as)) {
            SubmitAttempt sa = SubmitAttempt.builder()
                    .assignmentStatus(as)
                    .dateTimeStart(Instant.now())
                    .assignmentTimeElapsed(activeAssignment.getTimeElapsed())
                    .uuid(UUID.randomUUID())
                    .build();
            as.getSubmitAttempts().add(sa);

            return test(team, message).thenApply(sr -> {
                sa.setCompileAttempt(compileAttemptRepository.findByUuid(sr.getCompileResult()
                        .getCompileAttemptUuid()));
                sa.setTestAttempt(testAttemptRepository.findByUuid(sr.getTestResults().getTestAttemptUuid()));
                sa.setSuccess(sr.isSuccess());
                sa.setDateTimeEnd(Instant.now());
                submitAttemptRepository.save(sa);

                int remainingSubmits = getRemainingSubmits(as);
                try {
                    if (sr.isSuccess() || remainingSubmits <= 0) {
                        AssignmentStatus scored = scoreService.finalizeScore(as, activeAssignment);
                        return sr.toBuilder().score(scored.getAssignmentResult().getFinalScore())
                                .remainingSubmits(remainingSubmits).build();
                    }
                } catch (Exception e) {
                    log.error("Submit failed unexpectedly.", e);
                }

                return sr.toBuilder().remainingSubmits(remainingSubmits).build();
            }).thenApply(sr -> {
                messageService.sendSubmitFeedback(team, sr);
                return sr;
            });
        }
        return CompletableFuture.completedFuture(SubmitResult.builder().build());
    }

    private boolean isSubmitAllowedForTeam(AssignmentStatus as) {
        return getRemainingSubmits(as) > 0;
    }

    private int getRemainingSubmits(AssignmentStatus as) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        int maxSubmits = activeAssignment.getAssignmentDescriptor().getScoringRules().getMaximumResubmits() + 1;
        return maxSubmits - as.getSubmitAttempts().size();
    }
}
