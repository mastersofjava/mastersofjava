package nl.moj.server.config.properties;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Data;

@Data
public class Directories {

    @NotNull
    private Path baseDirectory;
    private String sessionDirectory = "sessions";
    private String teamDirectory = "teams";
    private String libDirectory = "lib";
    private String soundDirectory = "sounds";
    private String javadocDirectory = "javadoc";

    public Path getBaseDirectory() {
        if (baseDirectory.isAbsolute()) {
            return baseDirectory;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(baseDirectory);
    }

}