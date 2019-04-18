package nl.moj.server.runtime.repository;

import nl.moj.server.runtime.model.AssignmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentResultRepository extends JpaRepository<AssignmentResult,Long> {
}
