package nl.moj.server.message.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class SubmitEnded {

    private final MessageType messageType = MessageType.SUBMIT_ENDED;
    private final UUID team;
    private final boolean success;
    private final Long score;

}
