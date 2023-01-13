package nl.moj.common.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@JsonDeserialize(builder = JMSTestCaseResult.JMSTestCaseResultBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSTestCaseResult {

    @JsonProperty("run")
    private UUID runId;

    @JsonProperty("worker")
    private String worker;

    @JsonProperty("uuid")
    private UUID testCase;

    @JsonProperty("started")
    private Instant started;

    @JsonProperty("ended")
    private Instant ended;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("timeout")
    private boolean timeout;

    @JsonProperty("output")
    private String output;

    @JsonProperty("aborted")
    private boolean aborted;

    @JsonProperty("reason")
    private String reason;
}
