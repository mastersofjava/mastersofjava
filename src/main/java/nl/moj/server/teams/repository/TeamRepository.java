package nl.moj.server.teams.repository;

import java.util.List;
import java.util.UUID;

import nl.moj.server.teams.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Team findByName(String team);

    List<Team> findAllByRole(String role);

    Team findByUuid(UUID uuid);
}
