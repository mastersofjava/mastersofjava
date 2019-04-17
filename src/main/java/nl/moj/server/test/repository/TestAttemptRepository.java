package nl.moj.server.test.repository;

import nl.moj.server.test.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt,Long> {
    TestAttempt findByUuid(UUID testAttemptUuid);
}
