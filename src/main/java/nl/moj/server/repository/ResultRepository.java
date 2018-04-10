package nl.moj.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import nl.moj.server.model.Result;
import nl.moj.server.model.Team;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findAllByAssignment(String assignment);

    Result findByTeamAndAssignment(Team team, String assignment);

    @Query("SELECT SUM(r.score) FROM Result r WHERE r.team.name = :#{#team.name}")
    Integer getTotalScore(@Param("team") Team team);

    List<Result> findAllByOrderByTeamNameAsc();
}
