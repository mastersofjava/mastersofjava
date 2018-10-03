package nl.moj.server.runtime;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.*;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import org.apache.commons.io.IOUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
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
				.mediaType(resolveMediaType(ap))
				.build();
	}

	private MediaType resolveMediaType(Path file ) {
		try {
			TikaConfig config = TikaConfig.getDefaultConfig();
			Detector detector = config.getDetector();

			TikaInputStream stream = TikaInputStream.get(file);

			Metadata metadata = new Metadata();
			metadata.add(Metadata.RESOURCE_NAME_KEY, file.getFileName().toString());
			return detector.detect(stream, metadata);
		} catch( Exception e ) {
			log.warn("Unable to determine MediaType for {}, assuming text/plain.",e);
			return MediaType.TEXT_PLAIN;
		}
	}

	private String getShortName( Path file ) {
		return file.getFileName().toString().substring(0, file.getFileName().toString().indexOf("."));
	}

	private String getName(Path file) {
		return file.toString().substring(0, file.toString().indexOf(".")).replace(File.separatorChar, '.');
	}

	private byte[] readPathContent(Path p) {
		try {
			return IOUtils.toByteArray(Files.newInputStream(p, StandardOpenOption.READ));
		} catch (IOException e) {
			throw new RuntimeException("Unable to read assignment file " + p, e);
		}
	}
}