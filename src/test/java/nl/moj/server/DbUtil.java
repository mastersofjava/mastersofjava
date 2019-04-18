package nl.moj.server;

import lombok.AllArgsConstructor;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.repository.SubmitAttemptRepository;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.test.repository.TestAttemptRepository;
import nl.moj.server.test.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DbUtil {

	private CompetitionRepository competitionRepository;
	private AssignmentRepository assignmentRepository;
	private ResultRepository resultRepository;
	private TeamRepository teamRepository;
	private CompetitionSessionRepository competitionSessionRepository;
	private AssignmentStatusRepository assignmentStatusRepository;
	private AssignmentResultRepository assignmentResultRepository;
	private SubmitAttemptRepository submitAttemptRepository;
	private TestAttemptRepository testAttemptRepository;
	private TestCaseRepository testCaseRepository;
	private CompileAttemptRepository compileAttemptRepository;


	public void cleanup() {
		resultRepository.deleteAll();
		assignmentResultRepository.deleteAll();
		testCaseRepository.deleteAll();
		testAttemptRepository.deleteAll();
		compileAttemptRepository.deleteAll();
		submitAttemptRepository.deleteAll();
		assignmentStatusRepository.deleteAll();
		competitionSessionRepository.deleteAll();
		competitionRepository.deleteAll();
		assignmentRepository.deleteAll();
		teamRepository.deleteAll();
	}

}
