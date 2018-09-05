package nl.moj.server.compiler;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class CompileResult {

	private String result;

	@Builder.Default
	private List<String> tests = new ArrayList<>();

	private String user;
	
	private boolean successful;
	
	private Long scoreAtSubmissionTime;

}
