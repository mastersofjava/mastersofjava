package nl.moj.common.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonDeserialize(builder = JMSFile.JMSFileBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JMSFile {

    @JsonProperty("path")
    private String path;
    @JsonProperty("content")
    private String content;
}
