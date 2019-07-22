package nl.moj.server.feedback;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileSubmission {

	private UUID uuid;
	private String filename;
	private String content;
}
