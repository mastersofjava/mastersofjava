package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Builder
@ToString
public class TeamStartedTestingMessage {

	private final MessageType messageType = MessageType.TEAM_STARTED_TESTING;
	private final UUID uuid;
}
