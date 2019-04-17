package nl.moj.server.submit.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.ScoreService;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.service.TestResult;
import nl.moj.server.test.service.TestService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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

    public CompletableFuture<SubmitResult> compile(Team team, SourceMessage message) {
        competition.registerCompileRun(team);
        return compileService.compile(team, message)
                .whenComplete((compileResult, error) -> {
                    messageService.sendCompileFeedback(team, compileResult);
                    if (error != null) {
                        log.error("Compiling failed: {}", error.getMessage(), error);
                    }
                }).thenApply(cr -> SubmitResult.builder()
                        .success(cr.isSuccess())
                        .compileResult(cr)
                        .build());
    }

    public CompletableFuture<SubmitResult> test(Team team, SourceMessage message) {
        ActiveAssignment state = competition.getActiveAssignment();
        competition.registerTestRun(team);
        return compileAndTest(team, message, state.getTestFiles())
                .whenComplete((submitResult, error) -> {
                    if (error != null) {
                        log.error("Testing failed: {}", error.getMessage(), error);
                    }
                }).thenApply(r -> SubmitResult.builder()
                        .success(r.isSuccess())
                        .compileResult(r.getCompileResult())
                        .testResults(r.getTestResults())
                        .build());
    }


    public CompletableFuture<SubmitResult> submit(Team team, SourceMessage message) {
        if (isSubmitAllowedForTeam(team)) {
            competition.registerSubmit(team);
            ActiveAssignment state = competition.getActiveAssignment();
            AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(),state.getCompetitionSession(), team);

            SubmitAttempt sa = SubmitAttempt.builder()
                    .assignmentStatus(as)
                    .dateTimeStart(Instant.now())
                    .uuid(UUID.randomUUID())
                    .build();

            message.setTests(state.getSubmitTestFiles()
                    .stream()
                    .map(t -> t.getUuid().toString())
                    .collect(Collectors.toList()));
            return compileAndTest(team, message, state.getSubmitTestFiles())
                    .thenApply(ctr -> {

                        sa.setCompileAttempt(compileAttemptRepository.findByUuid(ctr.getCompileResult().getCompileAttemptUuid()));
                        sa.setTestAttempt(testAttemptRepository.findByUuid(ctr.getTestAttemptUuid()));
                        sa.setDateTimeEnd(Instant.now());
                        sa.setSuccess(ctr.isSuccess());
                        submitAttemptRepository.save(sa);

                        SubmitResult result = SubmitResult.builder()
                                .compileResult(ctr.getCompileResult())
                                .testResults(ctr.getTestResults())
                                .remainingSubmits(getRemainingSubmits(team))
                                .success(ctr.isSuccess())
                                .build();

                        try {
                            Score score = scoreService.calculateScore(team, state, ctr.isSuccess());
                            if (ctr.isSuccess() || getRemainingSubmits(team) <= 0) {
                                scoreService.registerScore(team, state.getAssignment(), competition.getCompetitionSession(), score);
                                competition.registerAssignmentCompleted(team, score.getInitialScore(), score.getTotalScore());
                                result = result.toBuilder().score(score.getTotalScore()).build();
                            }
                            messageService.sendSubmitFeedback(team, result);
                            return result;
                        } catch (Exception e) {
                            log.error("Submit failed unexpectedly.", e);
                        }
                        return result;
                    }).whenComplete((ctr, error) -> {
                        if (error != null) {
                            log.error("Submit failed: {}", error.getMessage(), error);
                        }
                    });
        }
        return CompletableFuture.completedFuture(SubmitResult.builder().build());
    }

    private boolean isSubmitAllowedForTeam(Team team) {
        return getRemainingSubmits(team) > 0;
    }

    private int getRemainingSubmits(Team team) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        CompetitionSession session = competition.getCompetitionSession();
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment.getAssignment(),
                session, team);
        int maxSubmits = activeAssignment.getAssignmentDescriptor().getScoringRules().getMaximumResubmits() + 1;
        return maxSubmits - as.getSubmitAttempts().size();
    }

    private CompletableFuture<CompileAndTestResult> compileAndTest(Team team, SourceMessage message, List<AssignmentFile> tests) {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment.getAssignment(),
                activeAssignment.getCompetitionSession(), team);

        return compileService.compile(team, message)
                .thenCompose(compileResult -> {
                    messageService.sendCompileFeedback(team, compileResult);
                    if (compileResult.isSuccess()) {
                        TestAttempt ta = testAttemptRepository.save(TestAttempt.builder()
                                .assignmentStatus(as)
                                .dateTimeStart(Instant.now())
                                .uuid(UUID.randomUUID())
                                .build());

                        return allTests(team, ta, tests.stream()
                                .filter(t -> message.getTests().contains(t.getUuid().toString()))
                                .collect(Collectors.toList()))
                                .thenApply(
                                        testResults -> CompileAndTestResult.builder()
                                                .compileResult(compileResult)
                                                .testAttemptUuid(ta.getUuid())
                                                .testResults(testResults)
                                                .build()
                                ).thenApply(crtr -> {
                                    updateAttemptEnd(ta.getId());
                                    return crtr;
                                });
                    } else {
                        return CompletableFuture.completedFuture(CompileAndTestResult.builder()
                                .compileResult(compileResult)
                                .build());
                    }
                });
    }

    // TODO maybe move this to test service.
    @SuppressWarnings("unchecked")
    private CompletableFuture<List<TestResult>> allTests(Team team, TestAttempt ta, List<AssignmentFile> tests) {
        messageService.sendTeamStartedTesting(team);
        List<CompletableFuture<TestResult>> cfs = new ArrayList<>();
        tests.forEach(t -> {
            cfs.add(testService.runTest(team, ta, t)
                    .thenApply(tr -> {
                        messageService.sendTestFeedback(team, tr);
                        return tr;
                    }));
        });
        return combine(cfs.toArray(new CompletableFuture[]{}));
    }

    private void updateAttemptEnd(Long id) {
        testAttemptRepository.findById(id).ifPresent(t -> {
            t.setDateTimeEnd(Instant.now());
            testAttemptRepository.save(t);
        });
    }

    private <T> CompletableFuture<List<T>> combine(CompletableFuture<T>... futures) {
        return CompletableFuture.allOf(futures)
                .thenApply(ignores -> Arrays.stream(futures)
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));


    }

    @Builder
    @Getter
    private static class CompileAndTestResult {
        private CompileResult compileResult;
        private UUID testAttemptUuid;
        private List<TestResult> testResults;

        public boolean isSuccess() {
            return compileResult != null && compileResult.isSuccess() &&
                    testResults.stream().allMatch(TestResult::isSuccess);
        }
    }
}
