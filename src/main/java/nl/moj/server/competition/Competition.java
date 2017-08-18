package nl.moj.server.competition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import nl.moj.server.files.AssignmentFile;

@Service
public class Competition {

	private String currentAssignment;
	
	private Map<String,Assignment> assignments;

	public String getCurrentAssignment() {
		return currentAssignment;
	}

	public void setCurrentAssignment(String currentAssignment) {
		this.currentAssignment = currentAssignment;
	}

	public Map<String, Assignment> getAssignments() {
		return assignments;
	}

	public void setAssignments(Map<String, Assignment> assignments) {
		this.assignments = assignments;
	}
	
	public void addAssignment(Assignment assignment) {
		if (assignments == null) {
			assignments = new HashMap<>();
		}
		assignments.put(assignment.getName(), assignment);
	}
	
	public void addAssignmentFile(AssignmentFile file) {
		if (assignments == null) {
			assignments = new HashMap<>();
		}
		Assignment assign;
		if (assignments.containsKey(file.getAssignment())) {
			assign = assignments.get(file.getAssignment());
			assign.getFilenames().forEach(fn -> System.out.println("in filenames:" + fn));
			System.out.println("adding " + file.getFilename());
			assign.addFilename(file.getFilename());
			assignments.replace(file.getAssignment(), assign);
		} else {
			List<String> filenames = new ArrayList<>();
			filenames.add(file.getFilename());
			assign = new Assignment(file.getAssignment(),filenames);
			assignments.put(file.getAssignment(), assign);
		}
	}

	public Set<String> getAssignmentNames() {
		return assignments.keySet();
	}
}
