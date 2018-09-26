package nl.moj.server.assignment.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Ejnar Kaekebeke
 */
@Data
public class ScoringRules {

    @JsonProperty("maximum-resubmits")
    private Integer maximumResubmits = 0;

    @JsonProperty("resubmit-penalty")
    private String resubmitPenalty = null;

    @JsonProperty("success-bonus")
    private Integer successBonus = null;

    @JsonProperty("test-penalty")
    private String testPenalty = null;

}
