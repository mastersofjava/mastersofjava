package nl.moj.server.test.repository;

import java.util.UUID;

import nl.moj.server.test.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    TestAttempt findByUuid(UUID testAttemptUuid);
}
