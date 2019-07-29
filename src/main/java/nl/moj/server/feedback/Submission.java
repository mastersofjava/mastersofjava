package nl.moj.server.feedback;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Submission {

    private UUID team;
    private List<FileSubmission> files;
}
