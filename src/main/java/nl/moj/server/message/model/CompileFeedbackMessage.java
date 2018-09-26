package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class CompileFeedbackMessage {

    private final MessageType messageType = MessageType.COMPILE;
    private final boolean success;
    private final String team;
    private final String message;

}
