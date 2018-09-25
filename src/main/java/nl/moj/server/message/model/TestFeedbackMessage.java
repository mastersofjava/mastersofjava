package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class TestFeedbackMessage {

	private final MessageType messageType = MessageType.TEST;
	private final String team;
	private final String message;
	private final String test;
	private final boolean success;

}
