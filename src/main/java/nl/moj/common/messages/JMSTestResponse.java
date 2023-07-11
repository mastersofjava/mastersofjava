package nl.moj.common.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of = {"aborted", "reason", "testCaseResults"})
public class JMSTestResponse extends JMSResponse {

    @JsonProperty("compileResponse")
    private JMSCompileResponse compileResponse;

    @JsonProperty("started")
    private Instant started;

    @JsonProperty("ended")
    private Instant ended;

    @JsonProperty("aborted")
    private boolean aborted;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("testCases")
    private List<JMSTestCaseResult> testCaseResults;
}
