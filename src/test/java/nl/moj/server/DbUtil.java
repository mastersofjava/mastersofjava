package nl.moj.server;

import lombok.AllArgsConstructor;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DbUtil {

	private CompetitionRepository competitionRepository;
	private AssignmentRepository assignmentRepository;
	private ResultRepository resultRepository;
	private TeamRepository teamRepository;

	public void cleanup() {
		resultRepository.deleteAll();
		competitionRepository.deleteAll();
		assignmentRepository.deleteAll();
		teamRepository.deleteAll();
	}

}
