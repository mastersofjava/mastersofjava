package nl.moj.server.repository;

import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.runtime.model.Result;
import nl.moj.server.teams.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

	@Query("SELECT SUM(r.score) FROM Result r WHERE r.team = :team AND r.competitionSession = :session")
	Integer getTotalScore(@Param("team") Team team, @Param("session") CompetitionSession session);

	List<Result> findAllByOrderByTeamNameAsc();

	List<Result> findAllByTeamAndCompetitionSession(Team team, CompetitionSession session);

	List<Result> findAllByAssignmentAndCompetitionSession(Assignment assignment, CompetitionSession competitionSession);

	Result findByTeamAndAssignmentAndCompetitionSession(Team team, Assignment assignment, CompetitionSession session);
}
