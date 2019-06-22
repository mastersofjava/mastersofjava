package nl.moj.server.rankings.model;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.competition.model.OrderedAssignment;

@Getter
@Builder
public class RankingHeader {

    private final OrderedAssignment orderedAssignment;
    private final String displayName;
}
