package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder(toBuilder = true)
public class AssignmentFile {

	private final String name;

	private final String filename;

	private final String content;

	private final AssignmentFileType fileType;

	private final String assignment;

	private final Path file;

	private final boolean readOnly;
}
