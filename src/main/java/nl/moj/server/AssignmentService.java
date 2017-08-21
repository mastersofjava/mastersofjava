package nl.moj.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;

@Service
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssignmentService {

	private List<AssignmentFile> assFiles = new ArrayList<>();
	@Autowired
	private Properties properties;
	
	public List<AssignmentFile> getJavaFiles() {
		return assFiles.stream().filter(f -> f.getFileType().equals(FileType.READONLY) || f.getFileType().equals(FileType.EDIT)).collect(Collectors.toList());
	}

	public List<AssignmentFile> getTestFiles() {
		return assFiles.stream().filter(f -> f.getFileType().equals(FileType.TEST)).collect(Collectors.toList());
	}

	public List<AssignmentFile> getJavaAndTestFiles() {
		List<AssignmentFile> files = getJavaFiles();
		files.addAll(getTestFiles());
		return files;
	}
	
	

	public void setAssignmentFiles(List<AssignmentFile> assFiles) {
		this.assFiles = assFiles;
	}

	public void addFiles(AssignmentFile... files) {
		for (AssignmentFile file : files) {
			assFiles.add(file);
		}
	}

	public void addFile(AssignmentFile file) {
		assFiles.add(file);
	}
	public Properties getProperties() {
		return properties;
	}

//	@Autowired
//	public void setProperties(Properties properties) {
//		this.properties = properties;
//	}

	public List<String> getEditableFileNames() {
		return Arrays.asList(properties.get("Editables").toString().split(","));
	}

	public List<String> getTestFileNames() {
		return Arrays.asList(properties.get("TestClasses").toString().split(","));
	}

	public List<String> getSubmitFileNames() {
		return Arrays.asList(properties.get("SubmitClasses").toString().split(","));
	}

	public List<String> getSolutionFileNames() {
		return Arrays.asList(properties.get("Solution").toString().split(","));
	}
	
	
}
