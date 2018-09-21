package nl.moj.server.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestFeedbackMessage extends FeedbackMessage {
    private String test;
    private boolean submit;
    private int score;

    public TestFeedbackMessage(String team, String test, String text, boolean success, Boolean submit, int score, int remainingResubmits) {
        super(team, text, success, remainingResubmits);
        this.test = test;
        this.submit = submit;
        this.setScore(score);
    }

}
