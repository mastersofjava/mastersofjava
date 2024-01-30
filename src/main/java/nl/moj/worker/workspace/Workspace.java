package nl.moj.worker.workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import nl.moj.common.assignment.descriptor.AssignmentDescriptor;

public interface Workspace extends AutoCloseable {

    AssignmentDescriptor getAssignmentDescriptor();

    Stream<Path> getSources() throws IOException;

    Path getRoot();

    Path getSourcesRoot();

    Path getTargetRoot();

}
