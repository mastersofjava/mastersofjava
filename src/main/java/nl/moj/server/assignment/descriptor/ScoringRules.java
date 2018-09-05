package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Ejnar Kaekebeke
 */
@Data
public class ScoringRules {

    @JsonProperty("maximum-resubmits")
    private Integer maximumResubmits;

    @JsonProperty("resubmit-penalty")
    private String resubmitPenalty;

    @JsonProperty("success-bonus")
    private Integer successBonus;

    @JsonProperty("test-penalty")
    private String testPenalty;

}
