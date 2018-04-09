package nl.moj.server.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResultRepository extends JpaRepository<Result, Long> {

    Result findByAssignment(String assignment);

    //    @Select("SELECT score FROM result WHERE team = #{team} AND assignment = #{assignment}")
    //    public Integer getScore(@Param("team") String team, @Param("assignment") String assignment);
    Result findByTeamAndAssignment(Team team, String assignment);

    //    	@Select("SELECT (*) FROM result WHERE team = #{team}")
    //    	public List<Result> getResults(@Param ("team") String team);
    List<Result> findAllByTeam(Team team);

    //	@Select("SELECT SUM(score) FROM result WHERE team = #{team}")
    //	public Integer getTotalScore(@Param("team") String team);
    	@Query("SELECT SUM(score) FROM result WHERE team = #{team.name}")
    	Integer getTotalScore(@Param("team") String teamName);

    //    	@Delete("DELETE FROM result WHERE assignment = #{assignment}")
    //    	public void deleteResultsByAssignment(@Param("assignment") String assignment);

    //    	@Update("UPDATE result SET score = #{score} where team = #{team} AND assignment = #{assignment}")
    //    	public void updateScore(@Param("team") String team, @Param("assignment") String assignment,	@Param("score") Integer score);
    //
    //    	@Update("INSERT INTO result(team, assignment, score) VALUES (#{team}, #{assignment}, #{score})")
    //    	public void insertScore(@Param("team") String team, @Param("assignment") String assignment,	@Param("score") Integer score);
    //
    //    	@Update("UPDATE result SET penalty = penalty + #{penalty} where team = #{team} AND assignment = #{assignment}")
    //    	public void incrementPenalty(@Param("team") String team, @Param("assignment") String assignment, @Param("penalty") Integer penalty);
    //
    //    	@Update("UPDATE result SET credit = credit - #{credit} where team = #{team} AND assignment = #{assignment}")
    //    	public void decrementCredit(@Param("team") String team, @Param("assignment") String assignment, @Param("penalty") Integer penalty);
    //
    //    	@Insert("INSERT INTO result(team, assignment) VALUES (#{team}, #{assignment})")
    //    	public void insertEmptyResult(@Param("team") String team, @Param("assignment") String assignment);
    //
    //    	@Insert("INSERT INTO result(team, assignment, score, penalty, credit) VALUES (#{team}, #{assignment}, #{score}, #{penalty}, #{credit})")
    //    	public void insertResult(Result r);
}
