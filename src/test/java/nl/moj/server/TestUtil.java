package nl.moj.server;

import java.nio.file.Path;
import java.nio.file.Paths;

import nl.moj.server.assignment.AssignmentServiceTest;

public class TestUtil {

    public static Path classpathResourceToPath(String resource) throws Exception {
        return Paths.get(AssignmentServiceTest.class.getResource(resource).toURI());
    }
}
