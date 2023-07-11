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
import java.util.UUID;

@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of={"success","timeout","aborted","reason"})
public class JMSCompileResponse extends JMSResponse {

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
