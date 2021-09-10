package nl.moj.server.submit.service;

import lombok.Builder;
import lombok.Data;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.teams.model.Team;
import nl.moj.server.user.model.User;

@Builder
@Data
public class SubmitRequest {

    private final Team team;
    private final User user;
    private final SourceMessage sourceMessage;
}
