package nl.moj.server.test;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TestResult {

	private String result;
	private String user;
	private boolean successful;
	private String testname;
//	private boolean submit;
	private Long scoreAtSubmissionTime;
	
}
