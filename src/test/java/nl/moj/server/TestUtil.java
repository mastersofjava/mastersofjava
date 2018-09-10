package nl.moj.server;

import nl.moj.server.assignment.AssignmentServiceTest;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtil {

	public static Path classpathResourceToPath(String resource) {
		return Paths.get(AssignmentServiceTest.class.getResource(resource).getPath());
	}
}
