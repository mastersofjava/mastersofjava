package nl.moj.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.moj.server.competition.Competition;
import nl.moj.server.files.AssignmentFile;

@Service
public class AssignmentService {

	@Autowired
	private Competition competition;
	
	public List<AssignmentFile> getJavaFiles() {
		return competition.getCurrentAssignment().getJavaFiles();
	}

	public List<AssignmentFile> getTestFiles() {
		return competition.getCurrentAssignment().getTestFiles();
	}

	public List<AssignmentFile> getJavaAndTestFiles() {
		return competition.getCurrentAssignment().getJavaAndTestFiles();
	}

	public List<AssignmentFile> getReadOnlyJavaFiles(){
		return competition.getCurrentAssignment().getReadOnlyJavaFiles();
	}
	
	public List<AssignmentFile> getReadOnlyJavaAndTestFiles(){
		return competition.getCurrentAssignment().getReadOnlyJavaAndTestFiles();
	}

	public List<String> getTestFileNames() {
		return competition.getCurrentAssignment().getTestClassesNames();
	}

	public List<String> getSubmitFileNames() {
		return competition.getCurrentAssignment().getSubmitFileNames();
	}

	public List<String> getSolutionFileNames() {
		return competition.getCurrentAssignment().getSolutionFileNames();
	}
	
	public List<String> getEditableFileNames() {
		return competition.getCurrentAssignment().getEditableFileNames();
	}
}
