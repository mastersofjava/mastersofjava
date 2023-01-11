package nl.moj.server.test.service;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.runtime.model.AssignmentFile;

@Getter
@Builder
public class TestCaseInput {

	private UUID teamUuid;
	private String teamName;
	private AssignmentFile file;
	private String assignmentName;
	private UUID competitionSessionUuid;
	private AssignmentDescriptor assignmentDescriptor;


}
