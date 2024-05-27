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
        @JsonSubTypes.Type(value = JMSCompileRequest.class, name = "compile"),
        @JsonSubTypes.Type(value = JMSTestRequest.class, name = "test"),
        @JsonSubTypes.Type(value = JMSSubmitRequest.class, name = "submit")
})
@Getter
@SuperBuilder
public abstract class JMSRequest {

    @JsonProperty("attempt")
    private UUID attempt;

}
