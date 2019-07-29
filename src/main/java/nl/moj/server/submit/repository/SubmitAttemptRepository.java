package nl.moj.server.submit.repository;

import nl.moj.server.submit.model.SubmitAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmitAttemptRepository extends JpaRepository<SubmitAttempt, Long> {
}
