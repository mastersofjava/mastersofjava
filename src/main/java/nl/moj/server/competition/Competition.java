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

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import nl.moj.server.DirectoriesConfiguration;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;
import nl.moj.server.persistence.TeamMapper;
import nl.moj.server.persistence.TestMapper;

@Service
public class Competition {

	private static final Logger log = LoggerFactory.getLogger(Competition.class);

	private AtomicReference<Assignment> currentAssignment = new AtomicReference<>(); // FIXME: access should be
																						// synchronized
	private ScheduledFuture<?> assignmentHandler;

	private ScheduledFuture<?> sound2MinHandler;
	private ScheduledFuture<?> sound1MinHandler;

	private ScheduledFuture<?> timeHandler;

	private ScheduledExecutorService scheduledExecutorService;

	private Stopwatch timer;

	private Map<String, Assignment> assignments;

	private AssignmentRepositoryService repo;

	private TestMapper testMapper;

	private ScoreService scoreService;

	private TeamMapper teamMapper;

	private FeedbackMessageController feedbackMessageController;

	private DirectoriesConfiguration directories;

	public Competition(AssignmentRepositoryService repo, TestMapper testMapper, ScoreService scoreService,
			TeamMapper teamMapper, FeedbackMessageController feedbackMessageController,
			DirectoriesConfiguration directories, ScheduledExecutorService scheduledExecutorService ) {
		super();
		this.repo = repo;
		this.testMapper = testMapper;
		this.teamMapper = teamMapper;
		this.scoreService = scoreService;
		this.feedbackMessageController = feedbackMessageController;
		this.directories = directories;
		this.scheduledExecutorService = scheduledExecutorService;
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
			File teamdir = FileUtils.getFile(directories.getBaseDirectory(), directories.getTeamDirectory(),
					team);
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
				scoreService.removeScoresForAssignment(k);
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
		try {
			if (assignments.containsKey(assignmentName)) {
				stopCurrentAssignment();
				this.currentAssignment.set(assignments.get(assignmentName));
			} else {
				return;
			}

			final Assignment assignment = currentAssignment.get();
			// remove old results
			scoreService.removeScoresForAssignment(assignment.getName());

			// initialize scores on 0.
			teamMapper.getAllTeams().forEach(t -> {
				scoreService.initializeScoreAtStart(t.getName(), assignment.getName());
			});
			assignment.setRunning(true);
			timer = Stopwatch.createStarted();
			Integer solutiontime = getCurrentAssignment().getSolutionTime();

			timeHandler = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						feedbackMessageController.sendRemainingTime(getRemainingTime(), solutiontime);
					} catch (Exception e) {
						log.error("Failed to send time update.", e);
					}
				}
			}, 0, 10, TimeUnit.SECONDS);
			startAssignmentRunnable(assignment, solutiontime);
			play2MinBeforeEndSound(solutiontime);
			play1MinBeforeEndSound(solutiontime);
			File gong = FileUtils
					.getFile("/home/mhayen/Workspaces/workspace-moj/server/src/main/resources/sounds/gong.wav");
			Media m = new Media(gong.toURI().toString());
			MediaPlayer player = new MediaPlayer(m);
			player.play();
			log.info("assignment started {}", assignment.getName());
		} catch (Exception e) {
			log.error("Starting assignment failed.", e);
		}
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
			assignmentHandler.cancel(true);
			timeHandler.cancel(true);
			log.info("assignment stopped {}", previousAssignment.get().getName());
			// set 0 score for teams that did not finish
			teamMapper.getAllTeams().stream()
					.filter(t -> !previousAssignment.get().getFinishedTeamNames().contains(t.getName()))
					.forEach(t -> scoreService.registerScoreAtSubmission(t.getName(), previousAssignment.get().getName(), 0));
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

	private void play2MinBeforeEndSound(Integer solutiontime) {
		sound2MinHandler = scheduledExecutorService.schedule(new Runnable() {
			@Override
			public void run() {
				System.out.println("hier");
				File gong = FileUtils.getFile(directories.getSoundDirectory(), "slowtictaclong.wav");
				Media m = new Media(gong.toURI().toString());
				MediaPlayer player = new MediaPlayer(m);
				player.play();
				System.out.println("hier");
				// soundHandler.cancel(false);
			}
		}, solutiontime - 120, TimeUnit.SECONDS);
	}

	private void play1MinBeforeEndSound(Integer solutiontime) {
		sound1MinHandler = scheduledExecutorService.schedule(new Runnable() {
			@Override
			public void run() {
				System.out.println("hier");
				File gong = FileUtils.getFile(directories.getSoundDirectory(), "slowtictaclong.wav");
				Media m = new Media(gong.toURI().toString());
				MediaPlayer player = new MediaPlayer(m);
				player.play();
				System.out.println("hier");
				// sound1MinHandler.cancel(false);
			}
		}, solutiontime - 60, TimeUnit.SECONDS);
	}

	private void startAssignmentRunnable(final Assignment assignment, Integer solutiontime) {
		assignmentHandler = scheduledExecutorService.schedule(new Runnable() {
			@Override
			public void run() {
				feedbackMessageController.sendStopToTeams(assignment.getName());
				assignmentHandler.cancel(false);
				stopCurrentAssignment();
			}
		}, solutiontime, TimeUnit.SECONDS);
	}
}
