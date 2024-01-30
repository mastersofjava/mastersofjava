package nl.moj.common.messages;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of = { "aborted", "reason", "testCaseResults" })
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
