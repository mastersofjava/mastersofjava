package nl.moj.server.message.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class TeamSubmitFeedbackMessage {

    private final MessageType messageType = MessageType.SUBMIT;
    private final UUID uuid;
    private final String team;
    private final String message;
    private final boolean success;
    private final long score;
    private final int remainingSubmits;
}
