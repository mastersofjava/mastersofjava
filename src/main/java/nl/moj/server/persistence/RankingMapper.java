package nl.moj.server.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import nl.moj.server.model.Ranking;

@Component
@Mapper
public interface RankingMapper {

	public List<Ranking> getRankings();
}
