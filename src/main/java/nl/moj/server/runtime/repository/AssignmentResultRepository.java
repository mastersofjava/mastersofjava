package nl.moj.server.runtime.repository;

import java.util.List;

import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.teams.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentResultRepository extends JpaRepository<AssignmentResult, Long> {


    @Query("SELECT a FROM AssignmentResult a WHERE a.assignmentStatus.team = :team " +
            "AND a.assignmentStatus.assignment = :assignment " +
            "AND a.assignmentStatus.competitionSession = :session")
    AssignmentResult findByTeamAndAssignmentAndCompetitionSession(Team team, Assignment assignment, CompetitionSession session);

    @Query("SELECT a FROM AssignmentResult a WHERE a.assignmentStatus.competitionSession = :session")
    List<AssignmentResult> findByCompetitionSession(@Param("session") CompetitionSession session);
}
