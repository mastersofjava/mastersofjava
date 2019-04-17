package nl.moj.server.runtime.repository;

import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.teams.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentStatusRepository extends JpaRepository<AssignmentStatus, Long> {

    List<AssignmentStatus> findByAssignmentAndCompetitionSession(Assignment assignment, CompetitionSession competitionSession);

    AssignmentStatus findByAssignmentAndCompetitionSessionAndTeam(Assignment assignment, CompetitionSession competitionSession, Team team);
}
