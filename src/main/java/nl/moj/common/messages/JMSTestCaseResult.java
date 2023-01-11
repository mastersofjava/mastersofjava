package nl.moj.common.messages;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import nl.moj.server.submit.service.TestCaseResult;

@Getter
@Builder
@JsonDeserialize(builder= JMSTestCaseResult.JMSTestCaseResultBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSTestCaseResult implements TestCaseResult {

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

    // this should not be needed it is linked directly to the UI
    @JsonProperty("name")
    private String name;
}
