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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import nl.moj.server.DirectoriesConfiguration;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.persistence.TeamMapper;
import nl.moj.server.persistence.TestMapper;

@Service
public class Competition {

	private static final Logger log = LoggerFactory.getLogger(Competition.class);

	private AtomicReference<Assignment> currentAssignment = new AtomicReference<>(); // FIXME: access should be
																						// synchronized
	private ScheduledFuture<?> handler;

	private static ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

	private Stopwatch timer;

	private Map<String, Assignment> assignments;

	private AssignmentRepositoryService repo;

	private TestMapper testMapper;

	private ResultMapper resultMapper;

	private TeamMapper teamMapper;
	
	private FeedbackMessageController feedbackMessageController;

	private DirectoriesConfiguration directories;

	public Competition(AssignmentRepositoryService repo, TestMapper testMapper,
			ResultMapper resultMapper, TeamMapper teamMapper, FeedbackMessageController feedbackMessageController,
			 DirectoriesConfiguration directories) {
		super();
		this.repo = repo;
		this.testMapper = testMapper;
		this.resultMapper = resultMapper;
		this.teamMapper = teamMapper;
		this.feedbackMessageController = feedbackMessageController;
		this.directories = directories;
	}

	/**
	 * Returns an immutable list of assignment files modified by the given team. The
	 * modified files are stored when they 'compile'
	 * 
	 * @param team
	 * @return a potentially empty list of files
	 */
	public List<AssignmentFile> getBackupFilesForTeam(String team) {
		Assignment assignment = getCurrentAssignment();
		if (assignment != null) {
			File teamdir = FileUtils.getFile(directories.getBaseDirectory(), directories.getAssignmentDirectory(), team);
			File sourcesdir = FileUtils.getFile(teamdir, "sources", assignment.getName());
			if (sourcesdir.exists()) {
				final List<AssignmentFile> assignmentFiles = FileUtils
						.listFiles(sourcesdir, TrueFileFilter.INSTANCE, null).stream().map(file -> {
							try {
								return new AssignmentFile(file.getName(),
										FileUtils.readFileToString(file, Charset.defaultCharset()), FileType.EDIT,
										assignment.getName(), file);
							} catch (IOException e) {
								log.error("Error retrieving backup files", e);
							}
							return null;
						}).collect(toImmutableList());
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

	public Assignment getCurrentAssignment() {
		return currentAssignment.get();
	}

	/**
	 * Starts the current assignment if one is set. Otherwise does nothing except
	 * logging a warning.
	 */
	public void startAssignment(String assignmentName) {
		if (assignments.containsKey(assignmentName)) {
			stopCurrentAssignment();
			this.currentAssignment.set(assignments.get(assignmentName));
		} else {
			return;
		}

		final Assignment assignment = currentAssignment.get();
		// remove old results
		resultMapper.deleteResultsByAssignment(assignment.getName());
		Integer solutiontime = getCurrentAssignment().getSolutionTime();
		handler = ex.schedule(new Runnable() {
			@Override
			public void run() {
				feedbackMessageController.sendStopToTeams(assignment.getName());
				handler.cancel(false);
				stopCurrentAssignment();
			}
		}, solutiontime, TimeUnit.SECONDS);

		assignment.setRunning(true);
		timer = Stopwatch.createStarted();
		log.info("assignment started {}", assignment.getName());
	}



	/**
	 * Stops the current assignment if one is set. Otherwise does nothing except
	 * logging a warning.
	 */
	public Optional<Assignment> stopCurrentAssignment() {
		final Optional<Assignment> previousAssignment = clearCurrentAssignment();

		if (previousAssignment.isPresent()) {
			previousAssignment.get().setRunning(false);
			previousAssignment.get().setCompleted(true);
			timer.stop();
			handler.cancel(true);
			log.info("assignment stopped {}", previousAssignment.get().getName());
			// set 0 score for teams that did not finish
			teamMapper.getAllTeams().stream()
					.filter(t -> !previousAssignment.get().getFinishedTeamNames().contains(t.getName()))
					.forEach(t -> resultMapper.insertScore(t.getName(), previousAssignment.get().getName(), 0));
		}
		return previousAssignment;
	}

	/**
	 * Returns the remaining time of the current assignment or 0, if none is active.
	 * 
	 * @return
	 */
	public Integer getRemainingTime() {
		final Assignment assignment = currentAssignment.get();
		if (assignment != null) {
			int solutiontime = assignment.getSolutionTime();
			int seconds = (int) timer.elapsed(TimeUnit.SECONDS);
			return solutiontime - seconds;
		} else {
			return 0;
		}
	}

	/**
	 * Clears the current assignment and returns its last value;
	 * 
	 * @return the stopped assignment
	 */
	public Optional<Assignment> clearCurrentAssignment() {
		return Optional.ofNullable(currentAssignment.getAndSet(null));
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
		return Optional.ofNullable(assignments).orElse(Collections.emptyMap()).values().stream()
				.filter(a -> a.isCompleted() || a.isRunning()).map(a -> a.getName()).sorted()
				.collect(Collectors.toList());
	}

	public List<ImmutablePair<String, Integer>> getAssignmentInfo() {
		return Optional.ofNullable(assignments).orElse(Collections.emptyMap()).values().stream()
				.map(v -> ImmutablePair.of(v.getName(), v.getSolutionTime())).sorted().collect(Collectors.toList());
	}
}
