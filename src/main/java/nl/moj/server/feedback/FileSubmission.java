package nl.moj.server.feedback;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileSubmission {

	private String filename;
	private String content;
}
