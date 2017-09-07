package nl.moj.server.competition;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
		return assFiles.stream()
				.filter(f -> f.getFileType().equals(FileType.EDIT) || f.getFileType().equals(FileType.READONLY))
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getTestFiles() {
		return assFiles.stream().filter(f -> f.getFileType().equals(FileType.TEST))
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getJavaAndTestFiles() {
		return assFiles.stream()
				.filter(f -> f.getFileType().equals(FileType.EDIT) || f.getFileType().equals(FileType.READONLY)
						|| f.getFileType().equals(FileType.TEST))
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getReadOnlyJavaFiles() {
		return assFiles.stream().filter(f -> f.getFileType().equals(FileType.READONLY))
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getReadOnlyJavaAndTestFiles() {
		return assFiles.stream()
				.filter(f -> f.getFileType().equals(FileType.READONLY) || f.getFileType().equals(FileType.TEST))
				.collect(Collectors.toList());
	}
	
	public List<AssignmentFile> getTestAndSubmitFiles() {
		return assFiles.stream()
				.filter(f -> f.getFileType().equals(FileType.TEST) || f.getFileType().equals(FileType.SUBMIT))
				.collect(Collectors.toList());
	}

	public List<AssignmentFile> getTaskFiles() {
		return assFiles.stream()
				.filter(f -> f.getFileType().equals(FileType.TASK))
				.collect(Collectors.toList());
	}
	
	public List<String> getTestFileNames() {
		return Arrays.asList(properties.get("testClasses").toString().split(","));
	}

	public List<String> getSubmitFileNames() {
		return Arrays.asList(properties.get("submitClasses").toString().split(","));
	}

	public List<String> getSolutionFileNames() {
		return Arrays.asList(properties.get("solution").toString().split(","));
	}

	public List<String> getEditableFileNames() {
		return Arrays.asList(properties.get("editables").toString().split(","));
	}

	public Integer getSolutionTime() {
		return Integer.valueOf(properties.get("solutiontime").toString());
	}
	
	
	
	public void setAssignmentFiles(List<AssignmentFile> assFiles) {
		this.assFiles = assFiles;
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
