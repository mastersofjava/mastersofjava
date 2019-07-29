package nl.moj.server.assignment.descriptor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestResources {

    @JsonProperty("base")
    private Path base;

    @JsonProperty("files")
    private List<Path> files;

    @JsonProperty("hidden-files")
    private List<Path> hiddenFiles;

    public List<Path> getFiles() {
        if (files == null) {
            return Collections.emptyList();
        }
        return files;
    }

    public List<Path> getHiddenFiles() {
        if (hiddenFiles == null) {
            return Collections.emptyList();
        }
        return hiddenFiles;
    }
}
