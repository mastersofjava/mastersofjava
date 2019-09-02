package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class StartAssignmentFailedMessage {

    private final MessageType messageType = MessageType.START_ASSIGNMENT_FAILED;

    private final String assignment;

    private final String cause;

}
