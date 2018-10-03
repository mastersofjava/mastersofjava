package nl.moj.server.submit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.CompileResult;
import nl.moj.server.compiler.CompileService;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.ScoreService;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.TestResult;
import nl.moj.server.test.TestService;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

	public void compile(Team team, SourceMessage message) {
		compileService.compile(team, message)
				.whenComplete((compileResult, error) -> {
					messageService.sendCompileFeedback(compileResult);
					if (error != null) {
						log.error("Compiling failed: {}", error.getMessage(), error);
					}
				});
	}

	public void test(Team team, SourceMessage message) {
		StopWatch sw = new StopWatch();
		sw.start();
		AssignmentState state = competition.getAssignmentState();
		compileAndTest(team, message, state, state.getTestFiles())
				.whenComplete((submitResult, error) -> {
					if (error != null) {
						log.error("Testing failed: {}", error.getMessage(), error);
					}
				});
	}


	public void submit(Team team, SourceMessage message) {
		if (competition.getAssignmentState().isSubmitAllowedForTeam(team)) {
			competition.registerSubmit(team);
			AssignmentState state = competition.getAssignmentState();
			compileAndTest(team, message, state, state.getSubmitTestFiles())
					.whenComplete((ctr, error) -> {
						if (error != null) {
							log.error("Submit failed: {}", error.getMessage(), error);
						}
						if (ctr != null) {
							try {
								Score score = scoreService.calculateScore(team, state, ctr.isSuccess());
								if (ctr.isSuccess() || state.getRemainingSubmits(team) <= 0) {
									scoreService.registerScore(team, state.getAssignment(), competition.getCompetitionSession(), score);
									competition.registerAssignmentCompleted(team, score.getInitialScore(), score.getTotalScore());
								}
								messageService.sendSubmitFeedback(SubmitResult.builder()
										.score(score.getTotalScore())
										.compileResult(ctr.getCompileResult())
										.testResults(ctr.getTestResults())
										.remainingSubmits(state.getRemainingSubmits(team))
										.success(ctr.isSuccess())
										.team(team)
										.build());
							} catch (Exception e) {
								log.error("Submit failed unexpectedly.", e);
							}
						}
					});
		}
	}

	private CompletableFuture<CompileAndTestResult> compileAndTest(Team team, SourceMessage message, AssignmentState state, List<AssignmentFile> tests) {
		return compileService.compile(team, message)
				.thenCompose(compileResult -> {
					messageService.sendCompileFeedback(compileResult);
					if (compileResult.isSuccessful()) {
						return allTests(team, tests)
								.thenApply(
										testResults -> CompileAndTestResult.builder()
												.compileResult(compileResult)
												.testResults(testResults)
												.build()
								);
					} else {
						return CompletableFuture.completedFuture(CompileAndTestResult.builder()
								.compileResult(compileResult)
								.build());
					}
				});
	}

	@SuppressWarnings("unchecked")
	private CompletableFuture<List<TestResult>> allTests(Team team, List<AssignmentFile> tests) {
		List<CompletableFuture<TestResult>> cfs = new ArrayList<>();
		tests.forEach(t -> cfs.add(testService.runTest(team, t)
				.thenApply(tr -> {
					messageService.sendTestFeedback(tr);
					return tr;
				})));
		return combine(cfs.toArray(new CompletableFuture[]{}));
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
		private List<TestResult> testResults;

		public boolean isSuccess() {
			return compileResult != null && compileResult.isSuccessful() &&
					testResults.stream().allMatch(TestResult::isSuccessful);
		}
	}
}
