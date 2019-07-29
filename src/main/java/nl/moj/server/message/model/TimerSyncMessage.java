package nl.moj.server.message.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TimerSyncMessage {

    private final MessageType messageType = MessageType.TIMER_SYNC;
    private final long remainingTime;
    private final long totalTime;
}