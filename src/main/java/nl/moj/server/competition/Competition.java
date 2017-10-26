package nl.moj.server.competition;

import com.google.common.base.Stopwatch;
import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TestMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class Competition {

	private static final Logger log = LoggerFactory.getLogger(Competition.class);

	private Assignment currentAssignment;

	private Stopwatch timer;

	private Map<String, Assignment> assignments;

	@Autowired
	private AssignmentRepositoryService repo;

	@Autowired
	private TestMapper testMapper;

	@Autowired
	private ResultMapper resultMapper;

	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

	public List<AssignmentFile> getBackupFilesForTeam(String team) {
		File teamdir = FileUtils.getFile(basedir, teamDirectory, team);
		File sourcesdir = FileUtils.getFile(teamdir, "sources", currentAssignment.getName());
		if (!sourcesdir.exists()) {
			return new ArrayList<>();
		}
		Collection<File> files = FileUtils.listFiles(sourcesdir, new IOFileFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return true;
			}

			@Override
			public boolean accept(File file) {
				// TODO Auto-generated method stub
				return true;
			}
		}, null);

		return files.stream().map(f -> {
			try {
				return new AssignmentFile(f.getName(), FileUtils.readFileToString(f, Charset.defaultCharset()),
						FileType.EDIT, currentAssignment.getName(), f);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toList());
	}

	public String cloneAssignmentsRepo(String repoName) {
		// verwijder bestaande als die bestaan
		if (assignments != null) {
			assignments.keySet().forEach((k) -> {
				testMapper.deleteTestsByAssignment(k);
				resultMapper.deleteResultsByAssignment(k);
			});
			assignments.clear();
		}
		return repo.cloneRemoteGitRepository(repoName) ? "repo succesvol gedownload" : "repo downloaden mislukt";
	}

	public void startCurrentAssignment() {
		if (currentAssignment == null) {
			throw new RuntimeException("currentAssignment not set");
		}
		currentAssignment.setRunning(true);
		timer = Stopwatch.createStarted();
		log.info("assignment started {}", currentAssignment.getName());
	}

	public void stopCurrentAssignment() {
		if (currentAssignment == null) {
			throw new RuntimeException("currentAssignment not set");
		}
		currentAssignment.setRunning(false);
		timer.stop();
		log.info("assignment stopped {}", currentAssignment.getName());
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

	public void setCurrentAssignment(String assignmentName) {
		if (assignments.containsKey(assignmentName)) {
			this.currentAssignment = assignments.get(assignmentName);
		}
	}

	public void clearCurrentAssignment() {
		this.currentAssignment = null;
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

	public List<String> getAssignmentNames() {
		return Optional.ofNullable(assignments).orElse(Collections.emptyMap()).keySet().stream().sorted()
				.collect(Collectors.toList());
	}
}
