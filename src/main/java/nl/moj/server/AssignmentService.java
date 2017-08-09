package nl.moj.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

@Service
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssignmentService {

	private List<JavaFile> assFiles = new ArrayList<>();
	@Autowired
	private Properties properties;
	
	public List<JavaFile> getAssignmentFiles() {
		return assFiles;
	}

	public void setAssignmentFiles(List<JavaFile> assFiles) {
		this.assFiles = assFiles;
	}

	public void addFiles(JavaFile... files) {
		for (JavaFile file : files) {
			assFiles.add(file);
		}
	}

	public void addFile(JavaFile file) {
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
	
	
}
