package nl.moj.common.messages;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonDeserialize(builder = JMSCompileRequest.JMSCompileRequestBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSCompileRequest {

    @JsonProperty("attempt")
    private UUID attempt;

    @JsonProperty("assignment")
    private UUID assignment;

    @JsonProperty("sources")
    private List<JMSFile> sources;

}
