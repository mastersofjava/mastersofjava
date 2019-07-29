package nl.moj.server.competition.repository;

import java.util.UUID;

import nl.moj.server.competition.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Competition findByUuid(UUID competitionUuid);

}
