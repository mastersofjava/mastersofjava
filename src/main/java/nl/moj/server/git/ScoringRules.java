package nl.moj.server.git;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Ejnar Kaekebeke
 */
@Data
@Embeddable
public class ScoringRules {

    @Column(name = "maximum_resubmits")
    @JsonProperty("maximum-resubmits")
    private Integer maximumResubmits;

    @Column(name = "resubmit_penalty")
    @JsonProperty("resubmit-penalty")
    private String resubmitPenalty;

    @Column(name = "success_bonus")
    @JsonProperty("success-bonus")
    private Integer successBonus;

    @Column(name = "test_penalty")
    @JsonProperty("test-penalty")
    private String testPenalty;

}
