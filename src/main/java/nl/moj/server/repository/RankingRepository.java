package nl.moj.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import nl.moj.server.model.Ranking;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

    List<Ranking> findAllByOrderByRankAsc();
}
