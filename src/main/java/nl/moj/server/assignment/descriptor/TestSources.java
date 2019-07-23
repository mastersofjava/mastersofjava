package nl.moj.server.assignment.descriptor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSources {

    @JsonProperty("base")
    private Path base;

    @JsonProperty("tests")
    private List<Path> tests;

    @JsonProperty("hidden-tests")
    private List<Path> hiddenTests;

    public List<Path> getTests() {
        if (tests == null) {
            return Collections.emptyList();
        }
        return tests;
    }

    public List<Path> getHiddenTests() {
        if (hiddenTests == null) {
            return Collections.emptyList();
        }
        return hiddenTests;
    }
}
