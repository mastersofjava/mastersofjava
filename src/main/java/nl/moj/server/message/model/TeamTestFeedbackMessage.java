package nl.moj.server.message.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
