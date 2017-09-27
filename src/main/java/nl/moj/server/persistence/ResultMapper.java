package nl.moj.server.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

import nl.moj.server.model.Result;
@Component
@Mapper
public interface ResultMapper {

	public List<Result> getAllResults();
	
	@Select("SELECT score FROM result WHERE team = #{team} AND assignment = #{assignment}")
	public Integer getScore(@Param("team") String team, @Param("assignment") String assignment);

	@Select("SELECT SUM(score) FROM result WHERE team = #{team}")
	public Integer getTotalScore(@Param("team") String team);

	@Update("UPDATE result SET score = #{score} where team = #{team} AND assignment = #{assignment}")
	public void updateScore(@Param("team") String team, @Param("assignment") String assignment,	@Param("score") Integer score);
	
	@Update("UPDATE result SET penalty = penalty + #{penalty} where team = #{team} AND assignment = #{assignment}")
	public void incrementPenalty(@Param("team") String team, @Param("assignment") String assignment, @Param("penalty") Integer penalty);

	@Update("UPDATE result SET credit = credit - #{credit} where team = #{team} AND assignment = #{assignment}")
	public void decrementCredit(@Param("team") String team, @Param("assignment") String assignment, @Param("penalty") Integer penalty);

	@Insert("INSERT INTO result(team, assignment) VALUES (#{team}, #{assignment})")
	public void insertResult(@Param("team") String team, @Param("assignment") String assignment);
	
	@Select("SELECT (*) FROM result WHERE team = #{team}")
	public List<Result> getResults(@Param ("team") String team);
	
	@Select("SELECT (*) FROM result WHERE team = #{team} AND assignment = #{assignment}")
	public Result getResult(@Param ("team") String team, @Param("assignment") String assignment);


}
