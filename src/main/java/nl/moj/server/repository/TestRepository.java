package nl.moj.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import nl.moj.server.model.Test;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findAllByAssignment(String assignment);
}
