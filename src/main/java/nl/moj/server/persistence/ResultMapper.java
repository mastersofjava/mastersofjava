package nl.moj.server.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import nl.moj.server.model.Result;
@Mapper
public interface ResultMapper {

	public List<Result> getAllResults();
	
}
