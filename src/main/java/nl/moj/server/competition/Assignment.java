package nl.moj.server.competition;

import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Assignment {

	private String name;

	private boolean running;

	private boolean completed;
	
	private List<String> filenames = new ArrayList<>();

	private Properties properties = new Properties();

	private List<AssignmentFile> assFiles = new ArrayList<>();

	private List<ImmutablePair<String, Integer>> finishedTeams = new ArrayList<>();;

	public Assignment(String name) {
		super();
		this.name = name;
	}

	public void addFinishedTeam(String team, Integer score) {
		finishedTeams.add(ImmutablePair.of(team, score));
	}

	public boolean isTeamFinished(String team) {
		return finishedTeams.stream().anyMatch(t -> t.left.equalsIgnoreCase(team));
	}

	public Integer getTeamSubmitTime(String team) {
		return finishedTeams.stream().filter(t -> t.left.equalsIgnoreCase(team)).map(t -> t.getRight()).findFirst().orElse(0);
	}
	
	public List<ImmutablePair<String, Integer>> getFinishedTeams() {
		return finishedTeams;
	}
	
	public List<String> getFinishedTeamNames() {
		return finishedTeams.stream().map(t -> t.getLeft()).collect(Collectors.toList());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
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
		if (isRunning()) {
			return assFiles.stream()
					.filter(f -> f.getFileType().equals(FileType.EDIT) || f.getFileType().equals(FileType.READONLY))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getEditableFiles() {
		if (isRunning()) {
			return assFiles.stream().filter(f -> f.getFileType().equals(FileType.EDIT)).collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getTestFiles() {
		if (isRunning()) {
			return assFiles.stream().filter(f -> f.getFileType().equals(FileType.TEST)).collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getJavaAndTestFiles() {
		if (isRunning()) {
			return assFiles
					.stream().filter(f -> f.getFileType().equals(FileType.EDIT)
							|| f.getFileType().equals(FileType.READONLY) || f.getFileType().equals(FileType.TEST))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getReadOnlyJavaFiles() {
		if (isRunning()) {
			return assFiles.stream().filter(f -> f.getFileType().equals(FileType.READONLY))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getReadOnlyJavaAndTestFiles() {
		if (isRunning()) {
			return assFiles.stream()
					.filter(f -> f.getFileType().equals(FileType.READONLY) || f.getFileType().equals(FileType.TEST))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getReadOnlyJavaAndTestAndSubmitFiles() {
		if (isRunning()) {
			return assFiles
					.stream().filter(f -> f.getFileType().equals(FileType.READONLY)
							|| f.getFileType().equals(FileType.TEST) || f.getFileType().equals(FileType.SUBMIT))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getSubmitFiles() {
		if (isRunning()) {
			return assFiles.stream().filter(f -> f.getFileType().equals(FileType.SUBMIT)).collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getTestAndSubmitFiles() {
		if (isRunning()) {
			return assFiles.stream()
					.filter(f -> f.getFileType().equals(FileType.TEST) || f.getFileType().equals(FileType.SUBMIT))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<AssignmentFile> getTaskFiles() {
		if (isRunning()) {
			return assFiles.stream().filter(f -> f.getFileType().equals(FileType.TASK)).collect(Collectors.toList());
		} else {
			return new ArrayList<AssignmentFile>();
		}
	}

	public List<String> getTestFileNames() {
		return Arrays.asList(properties.get("testClasses").toString().split(","));
	}

	public List<String> getTestNames() {
		return Arrays.asList(properties.get("testClasses").toString().split(",")).stream()
				.map(t -> t.substring(0, t.indexOf("."))).collect(Collectors.toList());
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

	public boolean hasTestPenalties() {
		return Boolean.valueOf(properties.get("testpenalties").toString());
	}

	public boolean hasTestCredits() {
		return Boolean.valueOf(properties.get("testcredits").toString());
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
