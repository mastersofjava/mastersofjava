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
	public Team findByName(@Param("name") String name);

	@Insert("INSERT INTO TEAM (name, password, role, country, company) VALUES (#{name}, #{password}, #{role}, #{country}, #{company})")
	public void insertTeam(Team team);
	
	
	
	@Select("SELECT * FROM TEAM")
	public List<Team> findAll();
	
	@Select("SELECT * FROM TEAM where role = 'ROLE_USER'")
	public List<Team> findAllUsers();

	@Select("SELECT name FROM TEAM where role = 'ROLE_USER'")
	public List<String> getAllUserNames();
	
	@Select("SELECT name, password, role FROM TEAM WHERE role='ROLE_USER'")
	public List<Team> getAllTeams();
}
