package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@ToString
public class Score {

    @Builder.Default
    private final Long initialScore = 0L;
    @Builder.Default
    private final Long submitBonus = 0L;
    @Builder.Default
    private final Long resubmitPenalty = 0L;
    @Builder.Default
    private final Long testPenalty = 0L;
    @Builder.Default
    private final Long testBonus = 0L;

    public Long getTotalScore() {
        Long score = initialScore + submitBonus + testBonus - resubmitPenalty - testPenalty;
        if (score < 0) {
            return 0L;
        }
        return score;
    }

    public Long getTotalBonus() { return submitBonus + testBonus; }

    public Long getTotalPenalty() {
        return resubmitPenalty + testPenalty;
    }
}
