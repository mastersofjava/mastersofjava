package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class SubmitFeedbackMessage {

	private final MessageType messageType = MessageType.SUBMIT;
	private final String team;
	private final String message;
	private final boolean success;
    private final long score;
    private final int remainingSubmits;
}
