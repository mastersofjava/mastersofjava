package nl.moj.server.message.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class TestingEnded {

    private final MessageType messageType = MessageType.TESTING_ENDED;
    private final UUID team;
    private final boolean success;

}
