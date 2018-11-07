package nl.moj.server.feedback;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class Submission {

	private UUID team;
	private List<FileSubmission> files;
}
