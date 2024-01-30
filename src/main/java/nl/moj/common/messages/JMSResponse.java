package nl.moj.common.messages;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = JMSCompileResponse.class, name = "compile"),
        @JsonSubTypes.Type(value = JMSTestResponse.class, name = "test"),
        @JsonSubTypes.Type(value = JMSSubmitResponse.class, name = "submit")
})
@Getter
@SuperBuilder
public abstract class JMSResponse {

    @JsonProperty("trace")
    private String traceId;

    @JsonProperty("worker")
    private String worker;

    @JsonProperty("attempt")
    private UUID attempt;

}
