package nl.moj.server.message.model.operations;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonDeserialize(builder= TestOperation.TestOperationBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestOperation {

    @JsonProperty("uuid")
    private UUID uuid;

    @JsonProperty("message")
    private String message;
}
