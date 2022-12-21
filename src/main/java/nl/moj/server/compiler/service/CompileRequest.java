package nl.moj.server.compiler.service;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.teams.model.Team;
import nl.moj.server.user.model.User;

@Data
@Builder
public class CompileRequest {

    private final User user;
    private final Team team;
    private final SourceMessage sourceMessage;
    private final Instant dateTimeSubmitted;

    public static CompileRequest from(SubmitRequest submitRequest) {
        return CompileRequest.builder()
                .user(submitRequest.getUser())
                .team(submitRequest.getTeam())
                .sourceMessage(submitRequest.getSourceMessage())
                .dateTimeSubmitted(Instant.now())
                .build();
    }
}
