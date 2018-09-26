package nl.moj.server.runtime;

import nl.moj.server.assignment.descriptor.*;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JavaAssignmentFileResolver {

	public List<AssignmentFile> resolve(AssignmentDescriptor ad) {
		List<AssignmentFile> originalAssignmentFiles = new ArrayList<>();

		// get all sources
		Sources sources = ad.getAssignmentFiles().getSources();
		sources.getEditable().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), sources.getBase(), p, AssignmentFileType.EDIT, false));
		});
		sources.getReadonly().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), sources.getBase(), p, AssignmentFileType.READONLY, true));
		});

		// get all resources
		Resources resources = ad.getAssignmentFiles().getResources();
		resources.getFiles().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), resources.getBase(), p, AssignmentFileType.RESOURCE, true));
		});

		// get all test sources
		TestSources testSources = ad.getAssignmentFiles().getTestSources();
		testSources.getTests().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testSources.getBase(), p, AssignmentFileType.TEST, true));
		});
		testSources.getHiddenTests().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testSources.getBase(), p, AssignmentFileType.HIDDEN_TEST, true));
		});

		// get all test resources
		TestResources testResources = ad.getAssignmentFiles().getTestResources();
		testResources.getFiles().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testResources.getBase(), p, AssignmentFileType.TEST_RESOURCE, true));
		});
		testResources.getHiddenFiles().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), testResources.getBase(), p, AssignmentFileType.HIDDEN_TEST_RESOURCE, true));
		});

		// get all solution files
		ad.getAssignmentFiles().getSolution().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), null, p, AssignmentFileType.SOLUTION, true));
		});
		// get the assignment
		originalAssignmentFiles.add(convertToAssignmentFile(ad.getName(), ad.getDirectory(), null, ad.getAssignmentFiles().getAssignment(), AssignmentFileType.TASK, true));

		return originalAssignmentFiles;
	}

	private AssignmentFile convertToAssignmentFile(String assignment, Path assignmentBase, Path prefix, Path file, AssignmentFileType type, boolean readOnly) {
		Path ap = assignmentBase;
		if (prefix != null) {
			ap = ap.resolve(prefix);
		}
		ap = ap.resolve(file);
		return AssignmentFile.builder()
				.assignment(assignment)
				.content(readPathContent(ap))
				.absoluteFile(ap)
				.file(file)
				.name(getName(file))
				.shortName(getShortName(file))
				.fileType(type)
				.readOnly(readOnly)
				.uuid(UUID.randomUUID())
				.build();
	}

	private String getShortName( Path file ) {
		return file.getFileName().toString().substring(0, file.getFileName().toString().indexOf("."));
	}

	private String getName(Path file) {
		return file.toString().substring(0, file.toString().indexOf(".")).replace(File.separatorChar, '.');
	}

	private String readPathContent(Path p) {
		try {
			return IOUtils.toString(Files.newInputStream(p, StandardOpenOption.READ), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read assignment file " + p, e);
		}
	}
}
