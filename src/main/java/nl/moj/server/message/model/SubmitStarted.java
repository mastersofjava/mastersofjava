package nl.moj.server.message.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class SubmitStarted {

    private final MessageType messageType = MessageType.SUBMIT_STARTED;
    private final UUID team;

}
