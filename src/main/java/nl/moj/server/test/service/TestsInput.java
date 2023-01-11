package nl.moj.server.test.service;

import java.util.List;

import lombok.Getter;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;

@Getter
public class TestsInput {
	List<TestCaseInput> testCases;
	private Long assignmentId;
	private Long competitionSessionId;
	private Long teamId;

	public TestsInput(TestRequest testRequest, List<AssignmentFile> tests, ActiveAssignment activeAssignment) {
		testCases = tests.stream().map(assignmentFile -> {
			return TestCaseInput.builder().assignmentDescriptor(activeAssignment.getAssignmentDescriptor())
					.assignmentName(activeAssignment.getAssignment().getName())
					.competitionSessionUuid(activeAssignment.getCompetitionSession().getUuid()).file(assignmentFile)
					.teamName(testRequest.getTeam().getName()).teamUuid(testRequest.getTeam().getUuid()).build();
		}).toList();

		assignmentId = activeAssignment.getAssignment().getId();
		competitionSessionId = activeAssignment.getCompetitionSession().getId();
		teamId = testRequest.getTeam().getId();
	}

}
