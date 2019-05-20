package nl.moj.server.compiler.repository;

import nl.moj.server.compiler.model.CompileAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompileAttemptRepository extends JpaRepository<CompileAttempt,Long> {
    CompileAttempt findByUuid(UUID compileAttemptUuid);
}
