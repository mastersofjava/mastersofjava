package nl.moj.server.competition;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;

public class Assignment {

	private String name;

	private List<String> filenames = new ArrayList<>();

	private Properties properties = new Properties();

	private List<AssignmentFile> assFiles = new ArrayList<>();

	public Assignment(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getFilenames() {
		return filenames;
	}

	public void setFilenames(List<String> filenames) {
		this.filenames = filenames;
	}

	public void addFilename(String filename) {
		filenames.add(filename);
	}

	public void addProperty(String name, String value) {
		properties.put(name, value);
	}

	public List<AssignmentFile> getJavaFiles() {
		return assFiles.stream().filter(f -> f.getFileType().equals(FileType.JAVA_SOURCE)).map(f -> checkReadOnly(f))
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getTestFiles() {
		return assFiles.stream().filter(f -> f.getFileType().equals(FileType.TEST)).map(f -> checkReadOnly(f)).collect(Collectors.toList());
	}

	public List<AssignmentFile> getJavaAndTestFiles() {
		return assFiles.stream()
				.filter(f -> f.getFileType().equals(FileType.JAVA_SOURCE) || f.getFileType().equals(FileType.TEST))
				.map(f -> checkReadOnly(f)).collect(Collectors.toList());
	}

	public List<AssignmentFile> getReadOnlyJavaFiles() {
		return assFiles.stream().filter(f -> f.getFileType().equals(FileType.JAVA_SOURCE)).map(f -> checkReadOnly(f))
				.filter(f -> f.isReadOnly())
				
				.collect(Collectors.toList());
	}
	
	public List<AssignmentFile> getReadOnlyJavaAndTestFiles() {
		return assFiles.stream()
				.filter(f -> f.getFileType().equals(FileType.JAVA_SOURCE) || f.getFileType().equals(FileType.TEST))
				.map(f -> checkReadOnly(f))
				.filter(f -> f.isReadOnly())
				.collect(Collectors.toList());
	}
	public void setAssignmentFiles(List<AssignmentFile> assFiles) {
		this.assFiles = assFiles;
	}

	private AssignmentFile checkReadOnly(AssignmentFile file) {
		List<String> editables =  Arrays.asList(properties.getProperty("editables").split(","));
		if (editables.contains(file.getName())) {
			file.setReadOnly(false);
		} else {
			file.setReadOnly(true);
		}
		return file;
	}
	
	
	public void parsePom(File pomXmlFile) {
		try {
			final Reader reader = new FileReader(pomXmlFile);
			final Model model;
			try {
				final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
				model = xpp3Reader.read(reader);
			} finally {
				reader.close();
			}

			properties = model.getProperties();
		} catch (XmlPullParserException ex) {
			throw new RuntimeException("Error parsing POM!", ex);
		} catch (final IOException ex) {
			throw new RuntimeException("Error reading POM!", ex);
		}
	}

	public void addFile(AssignmentFile file) {
		assFiles.add(file);
	}

}
