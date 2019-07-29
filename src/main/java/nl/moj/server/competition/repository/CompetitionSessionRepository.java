package nl.moj.server.competition.repository;

import java.util.List;
import java.util.UUID;

import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionSessionRepository extends JpaRepository<CompetitionSession, Long> {

    List<CompetitionSession> findByCompetition(Competition competition);

    CompetitionSession findByUuid(UUID session);
}

