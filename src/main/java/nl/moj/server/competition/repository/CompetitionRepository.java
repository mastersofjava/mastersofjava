package nl.moj.server.competition.repository;

import nl.moj.server.competition.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition,Long> {

	Competition findByUuid(UUID competitionUuid);
	
}
