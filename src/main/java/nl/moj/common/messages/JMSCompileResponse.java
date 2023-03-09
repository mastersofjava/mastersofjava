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
@JsonDeserialize(builder = JMSCompileResponse.JMSCompileResponseBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of={"success","timeout","aborted","reason"})
public class JMSCompileResponse {

    @JsonProperty("trace")
    private String traceId;

    @JsonProperty("worker")
    private String worker;

    @JsonProperty("attempt")
    private UUID attempt;

    @JsonProperty("aborted")
    private boolean aborted;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("output")
    private String output;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("timeout")
    private boolean timeout;

    @JsonProperty("started")
    private Instant started;

    @JsonProperty("ended")
    private Instant ended;
}
