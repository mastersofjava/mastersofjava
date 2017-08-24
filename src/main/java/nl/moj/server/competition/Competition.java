package nl.moj.server.competition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;

@Service
public class Competition {

	private Assignment currentAssignment;
	
	private Map<String,Assignment> assignments;

	public Assignment getCurrentAssignment() {
		return currentAssignment;
	}

	public void setCurrentAssignment(String assignmentName) {
		if (assignments.containsKey(assignmentName)) {
			this.currentAssignment = assignments.get(assignmentName);	
		}
	}
	
	public Assignment getAssignment(String name) {
		return assignments.get(name);
	}
	
	
	public void addAssignmentFile(AssignmentFile file) {
		if (assignments == null) {
			assignments = new HashMap<>();
		}
		Assignment assign;
		if (assignments.containsKey(file.getAssignment())) {
			assign = assignments.get(file.getAssignment());
			assign.addFilename(file.getFilename());
			assign.addFile(file);
			assignments.replace(file.getAssignment(), assign);
		} else {
			List<String> filenames = new ArrayList<>();
			filenames.add(file.getFilename());
			assign = new Assignment(file.getAssignment());
			assign.addFilename(file.getFilename());
			assign.addFile(file);
			assignments.put(file.getAssignment(), assign);
		}
		if (file.getFileType().equals(FileType.POM)) {
			assign.parsePom(file.getFile());
		}
	}

	public Set<String> getAssignmentNames() {
		return assignments.keySet();
	}
}
