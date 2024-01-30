package nl.moj.common.messages;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSSubmitRequest extends JMSRequest {

    @JsonProperty("assignment")
    private UUID assignment;

    @JsonProperty("tests")
    private List<JMSTestCase> tests;

    @JsonProperty("sources")
    private List<JMSFile> sources;
}
