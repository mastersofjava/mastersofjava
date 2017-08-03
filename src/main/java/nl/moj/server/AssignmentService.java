package nl.moj.server;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

@Service
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssignmentService {

	private List<JavaFile> assFiles = new ArrayList<>();

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
	
	
}
