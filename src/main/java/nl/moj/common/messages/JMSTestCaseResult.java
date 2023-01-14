package nl.moj.common.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@JsonDeserialize(builder = JMSTestCaseResult.JMSTestCaseResultBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of={"testCase","success","timeout","aborted","reason"})
public class JMSTestCaseResult {

    @JsonProperty("trace")
    private String traceId;

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
