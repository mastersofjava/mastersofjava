package nl.moj.common.messages;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import nl.moj.server.submit.service.CompileResponse;

@Getter
@Builder
@JsonDeserialize(builder= JMSCompileResponse.JMSCompileResponseBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSCompileResponse implements CompileResponse {

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
