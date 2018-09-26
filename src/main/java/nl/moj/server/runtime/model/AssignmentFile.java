package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class AssignmentFile {

	private final UUID uuid;

	private final String name;

	private final String shortName;

	private final String content;

	private final AssignmentFileType fileType;

	private final String assignment;

	private final Path absoluteFile;

	private final Path file;

	private final boolean readOnly;
}
