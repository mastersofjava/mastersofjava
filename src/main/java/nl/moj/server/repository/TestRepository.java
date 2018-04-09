package nl.moj.server.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test, Long> {

    //	@Delete("DELETE FROM test WHERE assignment = #{assignment}")
//    public void deleteTestsByAssignment(String assignment);

    Test findByAssignment(String assignment);

}
