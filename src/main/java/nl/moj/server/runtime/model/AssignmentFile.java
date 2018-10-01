package nl.moj.server.runtime.model;

import lombok.Builder;
import lombok.Data;
import org.apache.tika.mime.MediaType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class AssignmentFile {

	private final UUID uuid;

	private final String name;

	private final String shortName;

	private final byte[] content;

	private final AssignmentFileType fileType;

	private final String assignment;

	private final Path absoluteFile;

	private final Path file;

	private final MediaType mediaType;

	private final boolean readOnly;

	public String getContentAsString() {
		return new String(getContent(), StandardCharsets.UTF_8);
	}
}
