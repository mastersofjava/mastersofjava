package nl.moj.common.messages;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonDeserialize(builder = JMSTestCase.JMSTestCaseBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSTestCase {

    @JsonProperty("uuid")
    private UUID testCase;

    @JsonProperty("name")
    private String name;

}
