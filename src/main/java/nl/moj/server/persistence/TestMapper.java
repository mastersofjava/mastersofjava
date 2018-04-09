package nl.moj.server.persistence;

//import org.apache.ibatis.annotations.Delete;
//import org.apache.ibatis.annotations.Insert;
//import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import nl.moj.server.model.Test;

//@Component
//@Mapper
public interface TestMapper {

//	@Insert("insert into test ( team, assignment, testname, success, failure) values (#{team}, #{assignment}, #{testname}, #{success}, #{failure})")
	public void insertTest(Test test);

//	@Delete("DELETE FROM test WHERE assignment = #{assignment}")
	public void deleteTestsByAssignment(String assignment);

}
