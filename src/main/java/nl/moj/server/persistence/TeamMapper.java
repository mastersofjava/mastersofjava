package nl.moj.server.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import nl.moj.server.model.Team;
@Component
@Mapper
public interface TeamMapper {
	@Select("SELECT COUNT(*) FROM TEAM")
	public int countTeams();
	
    @Select("SELECT * FROM TEAM WHERE id = #{id}")
	public Team findById(@Param("id") Long id);
    
    @Select("SELECT * FROM TEAM WHERE name = #{name}")
    public Team findByName(@Param("name") String name);
    
    @Insert("INSERT INTO TEAM (name, password) VALUES (#{teamname}, #{password})")
    public void addTeam(@Param("teamname") String teamname, @Param("password") String password);
}
