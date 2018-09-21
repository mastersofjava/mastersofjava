package nl.moj.server.compiler;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class CompileResult {

	private String result;
	private String user;

	@Builder.Default
	private List<String> tests = new ArrayList<>();

	private boolean successful;
	private Long scoreAtSubmissionTime;
	private int remainingResubmits;

}
