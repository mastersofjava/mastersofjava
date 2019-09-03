package nl.moj.server.message.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class CompilingStarted {

    private final MessageType messageType = MessageType.COMPILING_STARTED;
    private final UUID team;

}
