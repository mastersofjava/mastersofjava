package nl.moj.server.runtime.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import nl.moj.server.competition.model.OrderedAssignment;

@Getter
@Builder
public class CompetitionState {

    @Builder.Default
    private List<OrderedAssignment> completedAssignments = new ArrayList<>();

}
