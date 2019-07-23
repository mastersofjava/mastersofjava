package nl.moj.server.message.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class TeamStartedTestingMessage {

    private final MessageType messageType = MessageType.TEAM_STARTED_TESTING;
    private final UUID uuid;
}
