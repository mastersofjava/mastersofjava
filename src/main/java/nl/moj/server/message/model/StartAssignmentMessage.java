package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class StartAssignmentMessage {

	private final MessageType messageType = MessageType.START_ASSIGNMENT;

	private final String assignment;

}
