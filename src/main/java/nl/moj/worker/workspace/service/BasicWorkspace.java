package nl.moj.worker.workspace.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import nl.moj.common.assignment.descriptor.*;

public class BasicWorkspace implements Workspace {

    private Path base;
    private Path sources;
    private Path target;
    private AssignmentDescriptor assignmentDescriptor;

    public BasicWorkspace(AssignmentDescriptor assignmentDescriptor, Map<Path,String> teamSources) throws IOException {
        this.base = Files.createTempDirectory("workspace");
        this.sources = this.base.resolve("sources");
        this.target = this.base.resolve("target");
        this.assignmentDescriptor = assignmentDescriptor;
        prepare();
        if( teamSources != null ) {
            importSourceFiles(teamSources);
        }
    }

    public void prepare() throws IOException {
        AssignmentFiles files = assignmentDescriptor.getAssignmentFiles();
        Sources sources = files.getSources();
        Path base = null;
        if( sources.getBase() != null ) {
            base = assignmentDescriptor.getDirectory().resolve(sources.getBase());
            for (Path p : sources.getEditable()) {
                importSourceFile(base.resolve(p), p);
            }
            for (Path p : sources.getHidden()) {
                importSourceFile(base.resolve(p), p);
            }
            for (Path p : sources.getReadonly()) {
                importSourceFile(base.resolve(p), p);
            }
        }

        Resources resources = files.getResources();
        if( resources.getBase() != null ) {
            base = assignmentDescriptor.getDirectory().resolve(resources.getBase());
            for (Path p : resources.getFiles()) {
                importResourceFile(base.resolve(p), p);
            }
        }

        TestSources testSources = files.getTestSources();
        if( testSources.getBase() != null ) {
            base = assignmentDescriptor.getDirectory().resolve(testSources.getBase());
            for (Path p : testSources.getTests()) {
                importSourceFile(base.resolve(p), p);
            }
            for (Path p : testSources.getHiddenTests()) {
                importSourceFile(base.resolve(p), p);
            }
            for (Path p : testSources.getInvisibleTests()) {
                importSourceFile(base.resolve(p), p);
            }
        }

        TestResources testResources = files.getTestResources();
        if( testResources.getBase() != null ) {
            base = assignmentDescriptor.getDirectory().resolve(testResources.getBase());
            for (Path p : testResources.getFiles()) {
                importResourceFile(base.resolve(p), p);
            }
            for (Path p : testResources.getHiddenFiles()) {
                importResourceFile(base.resolve(p), p);
            }
            for (Path p : testResources.getInvisibleFiles()) {
                importResourceFile(base.resolve(p), p);
            }
        }
    }

    public void importResourceFile(Path src, Path dest) throws IOException {
        try (InputStream data = Files.newInputStream(src)) {
            importResourceFile(data, dest);
        }
    }

    public void importResourceFile(InputStream data, Path p) throws IOException {
        Path tp = p;
        if (!p.startsWith(target)) {
            tp = target.resolve(p);
        }
        if( tp.getParent() != null ) {
            Files.createDirectories(tp.getParent());
        }
        Files.copy(data, tp, StandardCopyOption.REPLACE_EXISTING);
    }

    public void importSourceFile(Path src, Path dest) throws IOException {
        try (InputStream data = Files.newInputStream(src)) {
            importSourceFile(data, dest);
        }
    }

    public void importSourceFile(InputStream data, Path p) throws IOException {
        Path tp = p;
        if (!p.startsWith(sources)) {
            tp = sources.resolve(p);
        }
        if( tp.getParent() != null ) {
            Files.createDirectories(tp.getParent());
        }
        Files.copy(data, tp, StandardCopyOption.REPLACE_EXISTING);
    }

    public void importSourceFiles(Map<Path, String> sources) throws IOException {
        for (Map.Entry<Path, String> entry : sources.entrySet())
            importSourceFile(new ByteArrayInputStream(entry.getValue()
                    .getBytes(StandardCharsets.UTF_8)), entry.getKey());
    }

    public Stream<Path> getSources() throws IOException {
        return Files.walk(sources);
    }

    public void close() throws Exception {
        try (Stream<Path> walk = Files.walk(base)) {
            walk.sorted(Comparator.reverseOrder()).forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public Path getSourcesRoot() {
        return sources;
    }

    public Path getTargetRoot() {
        return target;
    }

    public AssignmentDescriptor getAssignmentDescriptor() {
        return assignmentDescriptor;
    }

    public Path getRoot() {
        return base;
    }
}
