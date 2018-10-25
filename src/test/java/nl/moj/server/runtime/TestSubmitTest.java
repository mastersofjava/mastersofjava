package nl.moj.server.runtime;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.SubmitService;
import nl.moj.server.submit.model.SourceMessage;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestSubmitTest extends BaseRuntimeTest {

	@Autowired
	private CompetitionRuntime competitionRuntime;

	@Autowired
	private SubmitService submitService;

	@Autowired
	private MojServerProperties mojServerProperties;

	@Test
	public void shouldUseSpecifiedAssignmentTestTimeout() {

		OrderedAssignment oa = getCompetition().getAssignments()
				.stream()
				.filter( a -> a.getAssignment().getName().equals("assignment-1"))
				.findFirst()
				.orElseThrow();

		competitionRuntime.startAssignment(oa.getAssignment().getName());

		AssignmentState state = competitionRuntime.getAssignmentState();
		Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
		timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());
		
		try {
			Map<String,String> files = state.getAssignmentFiles().stream()
					.filter( f -> f.getFileType() == AssignmentFileType.EDIT )
					.collect(Collectors.toMap( f -> f.getUuid().toString(), AssignmentFile::getContentAsString));

			SourceMessage src = new SourceMessage();
			src.setSources(files);
			src.setTests(Collections.singletonList(state.getTestFiles().get(0).getUuid().toString()));


			SubmitResult submitResult = submitService.test(getTeam(), src)
					.get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);

			Assertions.assertThat(submitResult.getTestResults().get(0).isSuccessful()).isFalse();
			Assertions.assertThat(submitResult.getTestResults().get(0).isTimeout()).isTrue();

		} catch (Exception e) {
			Assertions.fail("Caught unexpected exception.", e);
		}
	}
}