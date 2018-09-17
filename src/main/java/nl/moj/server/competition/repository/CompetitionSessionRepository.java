package nl.moj.server.competition.repository;

import nl.moj.server.competition.model.CompetitionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionSessionRepository extends JpaRepository<CompetitionSession,Long> {
}
