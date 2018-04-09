package nl.moj.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import nl.moj.server.model.Test;

public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findAllByAssignment(String assignment);
}
