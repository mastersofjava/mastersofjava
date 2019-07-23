package nl.moj.server.assignment.repository;

import nl.moj.server.assignment.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Assignment findByName(String name);
}
