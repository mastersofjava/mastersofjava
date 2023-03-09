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
@JsonDeserialize(builder = JMSSubmitResponse.JMSSubmitResponseBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of={"aborted","reason"})
public class JMSSubmitResponse {

    @JsonProperty("trace")
    private String traceId;

    @JsonProperty("worker")
    private String worker;

    @JsonProperty("attempt")
    private UUID attempt;

    @JsonProperty("testResponse")
    private JMSTestResponse testResponse;

    @JsonProperty("started")
    private Instant started;

    @JsonProperty("ended")
    private Instant ended;

    @JsonProperty("aborted")
    private boolean aborted;

    @JsonProperty("reason")
    private String reason;

}
