package nl.moj.server.teams.repository;

import nl.moj.server.teams.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team,Long> {

	Team findByName(String team);

	List<Team> findAllByRole(String role);
}
