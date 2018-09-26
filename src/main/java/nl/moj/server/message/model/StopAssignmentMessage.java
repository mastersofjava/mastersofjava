package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class StopAssignmentMessage {

	private final MessageType messageType = MessageType.STOP_ASSIGNMENT;

	private final String assignment;

}
