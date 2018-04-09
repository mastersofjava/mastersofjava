package nl.moj.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import nl.moj.server.model.Result;
import nl.moj.server.model.Team;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findAllByAssignment(String assignment);

    Result findByTeamAndAssignment(Team team, String assignment);

    @Query("SELECT SUM(score) FROM result WHERE team = #{team.name}")
    Integer getTotalScore(@Param("team") String teamName);

    List<Result> findAllOrderByTeam();
}
