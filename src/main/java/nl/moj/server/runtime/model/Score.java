package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
public class Score {
	
 	@Builder.Default
 	private final Long timeRemaining = 0L;
 	@Builder.Default
 	private final Long submitBonus = 0L;

 	public Long getFinalScore() {
 		return timeRemaining + submitBonus;
	}
}
