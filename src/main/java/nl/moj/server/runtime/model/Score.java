package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
public class Score {
	
 	@Builder.Default
 	private final Long initialScore = 0L;
 	@Builder.Default
 	private final Long submitBonus = 0L;
	@Builder.Default
 	private final Long resubmitPenalty = 0L;

 	public Long getTotalScore() {
 		return initialScore + submitBonus - resubmitPenalty;
	}

	public Long getTotalPenalty() {
 		return resubmitPenalty;
	}
}
