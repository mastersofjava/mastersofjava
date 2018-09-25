package nl.moj.server.submit;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.compiler.CompileResult;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.TestResult;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Getter
public class SubmitResult {

	private final Team team;
	private final int remainingSubmits;
	private final long score;

	private CompileResult compileResult;
	@Builder.Default
	private List<TestResult> testResults = new ArrayList<>();

	public boolean isSuccess() {
		return compileResult != null && compileResult.isSuccessful() &&
				testResults.stream().allMatch(TestResult::isSuccessful);
	}
}
