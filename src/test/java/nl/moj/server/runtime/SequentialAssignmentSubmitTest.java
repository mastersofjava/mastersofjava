package nl.moj.server.runtime;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.teams.model.Team;
import nl.moj.server.util.CompletableFutures;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SequentialAssignmentSubmitTest extends BaseRuntimeTest {

	@Autowired
	private CompetitionRuntime competitionRuntime;

	@Autowired
	private SubmitService submitService;

	@Autowired
	private MojServerProperties mojServerProperties;

	@Test
	public void sequentialExecutionShouldHaveNoOverlappingExecutionWindows() {

		Team team1 = getTeam();
		Team team2 = addTeam();

		OrderedAssignment oa = getAssignment("sequential");

		competitionRuntime.startAssignment(oa.getAssignment().getName());

		ActiveAssignment state = competitionRuntime.getActiveAssignment();
		Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
		timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

		try {
			// make sure team 1 runs for 0.1s
			Map<String, String> variables = new HashMap<>();
			variables.put("wait", "100");
			Map<String, String> files = getAssignmentFiles(state, variables);

			SourceMessage src1 = new SourceMessage();
			src1.setSources(files);
			src1.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

			// make sure team 2 runs for 0.05s
			variables = new HashMap<>();
			variables.put("wait", "50");
			files = getAssignmentFiles(state, variables);

			SourceMessage src2 = new SourceMessage();
			src2.setSources(files);
			src2.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

			// run them all at once and wait for the results.
			List<SubmitResult> results = CompletableFutures.allOf(submitService.test(team1, src1), submitService.test(team2, src2))
					.
							get(timeout.plusSeconds(20).toSeconds(), TimeUnit.SECONDS);

			// sequential execution means just a single completable future can run at any given time
			// this means there is no guarantee in the order, but none will run in the same time.
			SubmitResult t1result = results.stream()
					.filter(r -> r.getTeam().equals(team1.getUuid()))
					.findFirst()
					.orElse(null);
			SubmitResult t2result = results.stream()
					.filter(r -> r.getTeam().equals(team2.getUuid()))
					.findFirst()
					.orElse(null);

			assertThat(t1result).isNotNull();
			assertThat(t2result).isNotNull();

			// test that there is no overlap in execution windows.
			assertNoOverlappingExecutionWindows(t1result, t2result);


		} catch (Exception e) {
			Assertions.fail("Caught unexpected exception.", e);
		}
	}

	@Test
	public void shouldUseSpecifiedAssignmentTestTimeout() {

		OrderedAssignment oa = getAssignment("sequential");

		competitionRuntime.startAssignment(oa.getAssignment().getName());

		ActiveAssignment state = competitionRuntime.getActiveAssignment();
		Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
		timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

		try {
			Map<String, String> variables = new HashMap<>();
			variables.put("wait", Long.toString(timeout.toMillis()));
			Map<String, String> files = getAssignmentFiles(state, variables);

			SourceMessage src = new SourceMessage();
			src.setSources(files);
			src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));


			SubmitResult submitResult = submitService.test(getTeam(), src)
					.get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

			assertThat(submitResult.getTestResults().getResults().get(0).isSuccess()).isFalse();
			assertThat(submitResult.getTestResults().getResults().get(0).isTimeout()).isTrue();

		} catch (Exception e) {
			Assertions.fail("Caught unexpected exception.", e);
		}
	}

	@Test
	public void shouldGetPointsForSuccessOnFirstAttempt() {
		OrderedAssignment oa = getAssignment("sequential");

		competitionRuntime.startAssignment(oa.getAssignment().getName());

		ActiveAssignment state = competitionRuntime.getActiveAssignment();
		Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
		timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

		try {
			Map<String,String> files = getAssignmentFiles(state, new HashMap<>());

			SourceMessage src = new SourceMessage();
			src.setSources(files);
			src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));

			SubmitResult submitResult = submitService.submit(getTeam(), src)
					.get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

			assertThat(submitResult.isSuccess()).isTrue();
			assertThat(submitResult.getScore()).isGreaterThan(0);

		} catch (Exception e) {
			Assertions.fail("Caught unexpected exception.", e);
		}
	}
}
