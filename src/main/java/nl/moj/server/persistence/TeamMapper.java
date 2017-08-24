package nl.moj.server.persistence;

import java.util.List;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import nl.moj.server.model.Team;

@Component
@Mapper
public interface TeamMapper {
	@Select("SELECT COUNT(*) FROM TEAM")
	public int countTeams();

	@Select("SELECT name, password, role FROM TEAM WHERE name = #{name}")
	@Results(value={
			@Result(property="name", column="name"),
			@Result(property="password", column="password"),
			@Result(property="role", column="role"),
			@Result(property="results", javaType=List.class, column="name", many=@Many(select="getResults"))
	})
	public Team getTeam(@Param("name") String name);
	
    @Select("SELECT id, team, assignment, score FROM result WHERE team = #{team}")
    List<nl.moj.server.model.Result> getResults(String team);
	
	@Select("SELECT name, password, role FROM TEAM WHERE name = #{name}")
	public Team findByName(@Param("name") String name);

	@Insert("INSERT INTO TEAM (name, password, role) VALUES (#{teamname}, #{password}, #{role})")
	public void addTeam(@Param("teamname") String teamname, @Param("password") String password,
			@Param("role") String role);
	
	@Select("SELECT * FROM TEAM")
	public List<Team> findAll();
	
	@Select("SELECT * FROM TEAM where role = 'ROLE_USER'")
	public List<Team> findAllUsers();

	@Select("SELECT name FROM TEAM where role = 'ROLE_USER'")
	public List<String> getAllUserNames();
	
	@Select("SELECT name, password, role FROM TEAM WHERE role='ROLE_USER'")
	@Results(value={
			@Result(property="name", column="name"),
			@Result(property="password", column="password"),
			@Result(property="role", column="role"),
			@Result(property="results", javaType=List.class, column="name", many=@Many(select="getResults"))
	})
	public List<Team> getAllTeams();
}
