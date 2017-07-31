package nl.moj.server.compile;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "moj.server.compiler")
public final class CompilerProperties {

    private Path workDir;

    private Path outputDir;

    private boolean inheritClassPath;

    private List<String> classPath;

    /**
     * Returns the absolute path to the working directory.
     * @return the absolute path to the working directory, never <i>null</i>
     */
    public Path getWorkDir() {
        return workDir;
    }

    public void setWorkDir(final Path workDir) {
        requireNonNull(workDir);
        this.workDir = workDir.isAbsolute() ? workDir : Paths.get(System.getProperty("user.dir")).resolve(workDir);
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
    }

    public List<String> getClassPath() {
        return classPath;
    }

    public void setClassPath(List<String> classPath) {
        this.classPath = classPath;
    }

    public boolean inheritClassPath() {
        return inheritClassPath;
    }

    public void setInheritClassPath(boolean inheritClassPath) {
        this.inheritClassPath = inheritClassPath;
    }
}
