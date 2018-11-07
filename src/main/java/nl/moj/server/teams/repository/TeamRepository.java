package nl.moj.server.teams.repository;

import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team,Long> {

	Team findByName(String team);

	List<Team> findAllByRole(Role role);

	Team findByUuid(UUID uuid);
}
