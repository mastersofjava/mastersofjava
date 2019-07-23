package nl.moj.server;

import javax.transaction.Transactional;

import lombok.AllArgsConstructor;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DbUtil {

    private CompetitionRepository competitionRepository;
    private AssignmentRepository assignmentRepository;
    private TeamRepository teamRepository;
    private CompetitionSessionRepository competitionSessionRepository;
    private AssignmentStatusRepository assignmentStatusRepository;

    @Transactional
    public void cleanup() {
        assignmentStatusRepository.deleteAll();
        competitionSessionRepository.deleteAll();
        competitionRepository.deleteAll();
        assignmentRepository.deleteAll();
        teamRepository.deleteAll();
    }
}
