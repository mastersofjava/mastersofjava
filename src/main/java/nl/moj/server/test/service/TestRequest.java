package nl.moj.server.test.service;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.teams.model.Team;
import nl.moj.server.user.model.User;

@Data
@Builder
public class TestRequest {

    private final User user;
    private final Team team;
    private final SourceMessage sourceMessage;


    public static TestRequest from(SubmitRequest submitRequest) {
        return TestRequest.builder()
                .user(submitRequest.getUser())
                .team(submitRequest.getTeam())
                .sourceMessage(submitRequest.getSourceMessage())
                .build();
    }
}
