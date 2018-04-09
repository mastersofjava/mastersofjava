package nl.moj.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import nl.moj.server.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Team findByName(String name);

    List<Team> findAllByRole(String role);
}
