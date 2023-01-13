package nl.moj.worker.workspace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import nl.moj.common.assignment.descriptor.*;
import nl.moj.common.messages.JMSFile;
import nl.moj.worker.java.common.FileContent;

public class BasicWorkspace implements Workspace {

    private Path base;
    private Path sources;
    private Path target;
    private AssignmentDescriptor assignmentDescriptor;

    public BasicWorkspace(AssignmentDescriptor assignmentDescriptor, List<JMSFile> replacements) throws IOException {
        this.base = Files.createTempDirectory("workspace");
        this.sources = this.base.resolve("sources");
        this.target = this.base.resolve("target");
        this.assignmentDescriptor = assignmentDescriptor;
        prepare();
        if( replacements != null && !replacements.isEmpty() ) {
            replaceFiles(replacements);
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

    public void replaceFiles(List<JMSFile> files) throws IOException {
        for( JMSFile file : files ) {
            if( file.getType() == JMSFile.Type.SOURCE ) {
                importSourceFile(new ByteArrayInputStream(file.getContent()
                        .getBytes(StandardCharsets.UTF_8)), Paths.get(file.getPath()));
            }
            if( file.getType() == JMSFile.Type.RESOURCE ) {
                importResourceFile(new ByteArrayInputStream(file.getContent()
                        .getBytes(StandardCharsets.UTF_8)), Paths.get(file.getPath()));
            }
        }
    }

    public Stream<Path> getSources() throws IOException {
        return Files.walk(sources);
    }

    public void close() throws Exception {
        try (Stream<Path> walk = Files.walk(base)) {
            walk.sorted(Comparator.reverseOrder()).forEach(f -> {
                try {
                    System.out.println("DELETE: " + f.toAbsolutePath());
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
