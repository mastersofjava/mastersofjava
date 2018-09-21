package nl.moj.server.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskTimeMessage {
    private String remainingTime;
    private String totalTime;
}