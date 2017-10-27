package nl.moj.server.competition;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TestMapper;

@Service
public class Competition {

	private static final Logger log = LoggerFactory.getLogger(Competition.class);

	private AtomicReference<Assignment> currentAssignment = new AtomicReference<>();      // FIXME: access should be synchronized

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

	/**
	 * Returns an immutable list of assignment files modified by the given team. The modified files are stored when they 'compile'
	 * @param team
	 * @return a potentially empty list of files
	 */
	public List<AssignmentFile> getBackupFilesForTeam(String team) {
	    Assignment assignment = getCurrentAssignment();
	    if (assignment != null) {
	        File teamdir = FileUtils.getFile(basedir, teamDirectory, team);
	        File sourcesdir = FileUtils.getFile(teamdir, "sources", assignment.getName());
	        if (sourcesdir.exists()) {
	            final List<AssignmentFile> assignmentFiles = FileUtils.listFiles(sourcesdir, TrueFileFilter.INSTANCE, null).stream()
	                    .map(file -> {
	                        try {
	                            return new AssignmentFile(
	                                    file.getName(),
	                                    FileUtils.readFileToString(file, Charset.defaultCharset()),
	                                    FileType.EDIT,
	                                    assignment.getName(),
	                                    file);
	                        } catch (IOException e) {
	                            log.error("Error retrieving backup files", e);
	                        }
	                        return null;
	                    })
	                    .collect( toImmutableList() );
                return assignmentFiles;
	        }
	    }
	    return emptyList();
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

	/**
	 * Starts the current assignment if one is set.
	 * Otherwise does nothing except logging a warning.
	 */
	public void startCurrentAssignment() {
	    final Assignment assignment = getCurrentAssignment();
	    if (assignment!=null) {
	        assignment.setRunning(true);
	        timer = Stopwatch.createStarted();
	        log.info("assignment started {}", assignment.getName());
        } else {
            log.warn("Called startCurrentAssignment with currentAssignment==null");
        }
	}

    /**
     * Stops the current assignment if one is set.
     * Otherwise does nothing except logging a warning.
     */
	public Optional<Assignment> stopCurrentAssignment() {
	    final Optional<Assignment> previousAssignment = clearCurrentAssignment();
	    if (previousAssignment.isPresent()) {
	        previousAssignment.get().setRunning(false);
	        timer.stop();
	        log.info("assignment stopped {}", previousAssignment.get().getName());
        }
	    return previousAssignment;
	}


	public Integer getSecondsElapsed() {
		return (int) timer.elapsed(TimeUnit.SECONDS);
	}

	/**
	 * Returns the remaining time of the current assignment or 0, if none is active.
	 * @return
	 */
	public Integer getRemainingTime() {
        final Assignment assignment = getCurrentAssignment();
        if (assignment!=null) {
            int solutiontime = assignment.getSolutionTime();
            int seconds = getSecondsElapsed();
            return solutiontime - seconds;
        } else {
            return 0;
        }
	}

	public Assignment getCurrentAssignment() {
		return currentAssignment.get();
	}

	/**
	 * Activates the assignment <code>assignmentName</code> (if it exists).
	 * Any running assignment will be stopped.
	 *
	 * @param assignmentName   the name of the assignment to activate
	 * @return the previously active assignment (now stopped)
	 */
	public Optional<Assignment> setCurrentAssignment(String assignmentName) {
	    Assignment stoppedAssignment = null;
		if (assignments.containsKey(assignmentName)) {
		    stopCurrentAssignment();
		    stoppedAssignment = this.currentAssignment.getAndSet(assignments.get(assignmentName));
		}
		return Optional.ofNullable( stoppedAssignment );
	}

	/**
	 * Clears the current assignment and returns its last value;
	 * @return the stopped assignment
	 */
	public Optional<Assignment> clearCurrentAssignment() {
	    return Optional.ofNullable( currentAssignment.getAndSet(null) );
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
