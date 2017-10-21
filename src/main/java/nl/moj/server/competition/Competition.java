package nl.moj.server.competition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TestMapper;

@Service
public class Competition {

	private Assignment currentAssignment;

	private Stopwatch timer;

	private Map<String, Assignment> assignments;

	@Autowired
	private AssignmentRepositoryService repo;

	@Autowired
	private TestMapper testMapper;

	@Autowired
	private ResultMapper resultMapper;

	public String cloneAssignmentsRepo() {
		// verwijder bestaande als die bestaan
		if (assignments != null) {
			assignments.keySet().forEach((k) -> {
				testMapper.deleteTestsByAssignment(k);
				resultMapper.deleteResultsByAssignment(k);
			});
			assignments.clear();
		}
		return repo.cloneRemoteGitRepository() ? "repo succesvol gedownload" : "repo downloaden mislukt";
	}

	public void startCurrentAssignment() {
		if (currentAssignment == null) {
			throw new RuntimeException("currentAssignment not set");
		}
		currentAssignment.setRunning(true);
		timer = Stopwatch.createStarted();
	}

	public void stopCurrentAssignment() {
		if (currentAssignment == null) {
			throw new RuntimeException("currentAssignment not set");
		}
		currentAssignment.setRunning(false);
		timer = Stopwatch.createStarted();
	}
	
	public Integer getSecondsElapsed() {
		return (int) timer.elapsed(TimeUnit.SECONDS);
	}

	public Integer getRemainingTime() {
		int solutiontime = currentAssignment.getSolutionTime();
		int seconds = getSecondsElapsed();
		return solutiontime - seconds;
	}

	public Assignment getCurrentAssignment() {
		return currentAssignment;
	}

	public void clearCurrentAssignment() {
		this.currentAssignment = null;
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
		return Optional.ofNullable(assignments).orElse(Collections.emptyMap()).keySet();
	}
}
