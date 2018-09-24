package nl.moj.server.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompileFeedbackMessage extends FeedbackMessage {
    private boolean forTest;

    public CompileFeedbackMessage(String team, String text, boolean success, boolean forTest) {
        super(team, text, success, -1);
        this.forTest = forTest;
    }

}
