package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@Builder
public class TeamTestFeedbackMessage {

	private final MessageType messageType = MessageType.TEST;
	private final UUID uuid;
	private final String message;
	private final String test;
	private final boolean success;

}
