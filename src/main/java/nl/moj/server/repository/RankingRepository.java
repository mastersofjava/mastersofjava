package nl.moj.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import nl.moj.server.model.Ranking;

public interface RankingRepository extends JpaRepository<Ranking, Long> {


    List<Ranking> findAllOrderByRank();
}
