package nl.moj.server.message;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackMessage {
    private String team;
    private String text;
    private boolean success;
    private int remainingResubmits;

    public FeedbackMessage(String team, String text, boolean success, int remainingResubmits) {
        this.team = team;
        this.text = text;
        this.success = success;
        this.remainingResubmits = remainingResubmits;

    }
}
