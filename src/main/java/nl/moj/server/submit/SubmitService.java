package nl.moj.server.submit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.compiler.CompileService;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.ScoreService;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.Score;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.TestService;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
		StopWatch sw = new StopWatch();
		sw.start();
		AssignmentState state = competition.getAssignmentState();
		compileService.compile(team, message)
				.thenCompose(compileResult -> CompletableFuture.completedFuture(SubmitResult.builder()
						.remainingSubmits(state.getRemainingSubmits(team))
						.score(0L)
						.compileResult(compileResult)
						.team(team)
						.build()))
				.whenComplete((submitResult, error) -> {
					sw.stop();
					log.info("compiling took: {}s", sw.getTime(TimeUnit.SECONDS));

					if (error != null) {
						log.error("Compiling failed: {}", error.getMessage(), error);
					}
					if (submitResult != null) {
						try {
							messageService.sendCompileFeedback(submitResult);
						} catch (Exception e) {
							log.error("Compiling failed unexpectedly.", e);
						}
					}
				});
	}

	public void test(Team team, SourceMessage message) {
		StopWatch sw = new StopWatch();
		sw.start();
		AssignmentState state = competition.getAssignmentState();
		compileAndTest(team, message, state, state.getTestFiles())
				.whenComplete((submitResult, error) -> {
					sw.stop();
					log.info("testing took: {}s", sw.getTime(TimeUnit.SECONDS));

					if (error != null) {
						log.error("Testing failed: {}", error.getMessage(), error);
					}
					if (submitResult != null) {
						try {
							messageService.sendCompileFeedback(submitResult);
							messageService.sendTestFeedback(submitResult);
						} catch (Exception e) {
							log.error("Testing failed unexpectedly.", e);
						}
					}
				});
	}


	public void submit(Team team, SourceMessage message) {
		if (competition.getAssignmentState().isSubmitAllowedForTeam(team)) {
			StopWatch sw = new StopWatch();
			sw.start();


			competition.registerSubmit(team);
			AssignmentState state = competition.getAssignmentState();
			compileAndTest(team, message, state, state.getSubmitTestFiles())
					.whenComplete((submitResult, error) -> {
						sw.stop();
						log.info("submit took: {}s", sw.getTime(TimeUnit.SECONDS));

						if (error != null) {
							log.error("Submit failed: {}", error.getMessage(), error);
						}
						if (submitResult != null) {
							try {
								Score score = scoreService.calculateScore(team, state, competition.getCompetitionSession(), submitResult.isSuccess());
								if (submitResult.isSuccess() || state.getRemainingSubmits(team) <= 0) {
									scoreService.registerScore(team, state.getAssignment(), competition.getCompetitionSession(), score);
									competition.registerAssignmentCompleted(team,score.getTimeRemaining(), score.getFinalScore());
								}
								messageService.sendSubmitFeedback(submitResult.toBuilder().score(score.getFinalScore()).build());
							} catch (Exception e) {
								log.error("Submit failed unexpectedly.", e);
							}
						}
					});
		}
	}

	private CompletableFuture<SubmitResult> compileAndTest(Team team, SourceMessage message, AssignmentState state, List<AssignmentFile> tests) {
		return compileService.compile(team, message)
				.thenCompose(compileResult -> {
					if (compileResult.isSuccessful()) {
						return testService.runTests(team, tests)
								.thenCompose(
										testResults -> CompletableFuture.completedFuture(SubmitResult.builder()
												.remainingSubmits(state.getRemainingSubmits(team))
												.team(team)
												.compileResult(compileResult)
												.testResults(testResults)
												.build()));
					} else {
						return CompletableFuture.completedFuture(SubmitResult.builder()
								.remainingSubmits(state.getRemainingSubmits(team))
								.score(0L)
								.compileResult(compileResult)
								.team(team)
								.build());
					}
				});
	}
}
