package nl.moj.server.submit.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.ScoreService;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.service.TestResults;
import nl.moj.server.test.service.TestService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class SubmitService {

    private final CompetitionRuntime competition;
    private final CompileService compileService;
    private final TestService testService;
    private final ScoreService scoreService;
    private final MessageService messageService;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final CompileAttemptRepository compileAttemptRepository;
    private final SubmitAttemptRepository submitAttemptRepository;
    private final AssignmentResultRepository assignmentResultRepository;

    @Async("compiling")
    @Transactional
    public CompletableFuture<SubmitResult> compileAsync(Team team, SourceMessage message) {
        CompileResult cr = compileInternal(team, message);
        return CompletableFuture.completedFuture(SubmitResult.builder()
                .success(cr.isSuccess())
                .compileResult(cr)
                .build());
    }

    private CompileResult compileInternal(Team team, SourceMessage message) {
        CompileResult cr = compileService.compileSync(team, message);
        messageService.sendCompileFeedback(team, cr);
        return cr;
    }

    @Async("testing")
    @Transactional
    public CompletableFuture<SubmitResult> testAsync(Team team, SourceMessage message) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        SubmitResult sr = SubmitResult.builder()
                .success(false)
                .build();

        //compile
        CompileResult cr = compileInternal(team, message);
        sr = sr.toBuilder().compileResult(cr).build();

        if (cr.isSuccess()) {
            TestResults trs = testInternal(team, activeAssignment.getTestFiles().stream()
                    .filter(t -> message.getTests().contains(t.getUuid().toString()))
                    .collect(Collectors.toList()));
            sr = sr.toBuilder()
                    .success(trs.isSuccess())
                    .testResults(trs.getResults())
                    .build();
        }

        return CompletableFuture.completedFuture(sr);
    }

    private TestResults testInternal(Team team, List<AssignmentFile> tests) {
        TestResults trs = testService.runTestsSync(team, tests);
        trs.getResults().forEach(tr -> messageService.sendTestFeedback(team, tr));
        return trs;
    }


    @Async("submitting")
    @Transactional
    public CompletableFuture<SubmitResult> submitAsync(Team team, SourceMessage message) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment
                .getAssignment(), activeAssignment.getCompetitionSession(), team);

        if (isSubmitAllowedForTeam(as)) {
            SubmitAttempt sa = SubmitAttempt.builder()
                    .assignmentStatus(as)
                    .dateTimeStart(Instant.now())
                    .assignmentTimeElapsed(activeAssignment.getTimeElapsed())
                    .uuid(UUID.randomUUID())
                    .build();
            as.getSubmitAttempts().add(sa);

            SubmitResult sr = SubmitResult.builder()
                    .success(false)
                    .build();

            //compile
            CompileResult cr = compileInternal(team, message);
            sr = sr.toBuilder().compileResult(cr).build();
            sa.setCompileAttempt(compileAttemptRepository.findByUuid(cr.getCompileAttemptUuid()));

            if (cr.isSuccess()) {
                TestResults trs = testInternal(team, activeAssignment.getSubmitTestFiles());
                sa.setTestAttempt(testAttemptRepository.findByUuid(trs.getTestAttemptUuid()));
                sa.setSuccess(trs.isSuccess());
                sa.setDateTimeEnd(Instant.now());
                submitAttemptRepository.save(sa);

                sr = sr.toBuilder()
                        .success(trs.isSuccess())
                        .testResults(trs.getResults())
                        .build();

                try {
                    if (trs.isSuccess() || getRemainingSubmits(as) <= 0) {
                        as = scoreService.finalizeScore(as,activeAssignment);
                        sr = sr.toBuilder().score(as.getAssignmentResult().getFinalScore()).build();
                    }
                } catch (Exception e) {
                    log.error("Submit failed unexpectedly.", e);
                }
            }

            sr = sr.toBuilder().remainingSubmits(getRemainingSubmits(as)).build();
            messageService.sendSubmitFeedback(team, sr);

            return CompletableFuture.completedFuture(sr);
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
