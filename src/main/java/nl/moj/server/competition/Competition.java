package nl.moj.server.competition;

import lombok.RequiredArgsConstructor;
import nl.moj.server.DirectoriesConfiguration;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.AssignmentFileVisitor;
import nl.moj.server.files.FileType;
import nl.moj.server.repository.TeamRepository;
import nl.moj.server.repository.TestRepository;
import nl.moj.server.sound.SoundService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
public class Competition {

	private static final Logger log = LoggerFactory.getLogger(Competition.class);

    @Value("${moj.server.directories.assignmentDirectory}")
    private String assignmentDirectory;

    private final AssignmentRepositoryService repo;

    private final TestRepository testRepository;

    private final ScoreService scoreService;

    private final TeamRepository teamRepository;

    private final FeedbackMessageController feedbackMessageController;

    private final DirectoriesConfiguration directories;

    private final SoundService soundService;

    private final ScheduledExecutorService scheduledExecutorService;

    private AtomicReference<Assignment> currentAssignment = new AtomicReference<>(); // FIXME: access should be
																						// synchronized
	private ScheduledFuture<?> assignmentHandler;

	private ScheduledFuture<?> soundHandler;

	private ScheduledFuture<?> timeHandler;

	private StopWatch timer;

	private Map<String, Assignment> assignments;

	/**
	 * Returns a list of assignment files modified by the given team. The
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
                return FileUtils
                        .listFiles(sourcesdir, TrueFileFilter.INSTANCE, null).stream().map(file -> {
                            try {
                                return new AssignmentFile(file.getName(),
                                        FileUtils.readFileToString(file, Charset.defaultCharset()), FileType.EDIT,
                                        assignment.getName(), file);
                            } catch (IOException e) {
                                log.error("Error retrieving backup files", e);
                            }
                            return null;
                        }).collect(Collectors.toList());
			}
		}
		return emptyList();
	}

	public String cloneAndInitAssignmentsFromRepo(String repoName) {

		if (cloneAssignmentsRepo(repoName)) {
			if(initAssignments()) {
				return "repo gedownload en geinitialiseerd";
			}

			return "fout tijdens het initialiseren van de assigments";
		}

		return "repo downloaden mislukt";
	}

	private boolean initAssignments() {
		log.info("Initialising assignments");
		AssignmentFileVisitor visitor = new AssignmentFileVisitor(assignmentDirectory, this, testRepository, teamRepository);
		Path assignmentPath = Paths.get(directories.getBaseDirectory(), directories.getAssignmentDirectory());

		try {
			Files.walkFileTree(assignmentPath, visitor);
		} catch (IOException e) {
			return false;
		}


		return true;
	}

	private boolean cloneAssignmentsRepo(String repoName) {
		// verwijder bestaande als die bestaan
		if (assignments != null) {
			assignments.keySet().forEach((k) -> {
                testRepository.findAllByAssignment(k).forEach(testRepository::delete);
				scoreService.removeScoresForAssignment(k);
			});
			assignments.clear();
		}
		return repo.cloneRemoteGitRepository(repoName); // ? "repo succesvol gedownload" : "repo downloaden mislukt";
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
			teamRepository.findAllByRole("ROLE_USER").forEach(t -> {
				scoreService.initializeScoreAtStart(t.getName(), assignment.getName());
			});
			assignment.setRunning(true);
			timer = StopWatch.createStarted();
			Integer solutiontime = getCurrentAssignment().getSolutionTime();

			startAssignmentRunnable(assignment, solutiontime);
			scheduleBeforeEndSound(solutiontime,120);
			scheduleBeforeEndSound(solutiontime, 60);
			startTimeSync(solutiontime);
			soundService.playGong();
			log.info("assignment started {}", assignment.getName());
		} catch (Exception e) {
			log.error("Starting assignment failed.", e);
		}
	}

	private void startTimeSync(Integer solutiontime) {
		timeHandler = scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                feedbackMessageController.sendRemainingTime(getRemainingTime(), solutiontime);
            } catch (Exception e) {
                log.error("Failed to send time update.", e);
            }
        }, 1, 10, TimeUnit.SECONDS);
	}

	/**
	 * Stops the current assignment if one is set. Otherwise does nothing except
	 * logging a warning.
	 */
	public Optional<Assignment> stopCurrentAssignment() {
		final Optional<Assignment> previousAssignment = Optional.ofNullable(currentAssignment.getAndSet(null));

		if (previousAssignment.isPresent()) {
			previousAssignment.get().setRunning(false);
			previousAssignment.get().setCompleted(true);
			timer.stop();
			assignmentHandler.cancel(true);
			timeHandler.cancel(true);
			log.info("assignment stopped {}", previousAssignment.get().getName());
			// set 0 score for teams that did not finish
			teamRepository.findAllByRole("ROLE_USER").stream()
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
			if (timer == null) {
				return 0;
			}
			int seconds = (int) timer.getTime(TimeUnit.SECONDS);
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
				.filter(a -> a.isCompleted() || a.isRunning()).map(Assignment::getName).sorted()
				.collect(Collectors.toList());
	}

	public List<ImmutablePair<String, Integer>> getAssignmentInfo() {
		return Optional.ofNullable(assignments).orElse(Collections.emptyMap()).values().stream()
				.map(v -> ImmutablePair.of(v.getName(), v.getSolutionTime())).sorted().collect(Collectors.toList());
	}

	private void scheduleBeforeEndSound(Integer solutiontime, int secondsBeforeEnd ) {
		soundHandler = scheduledExecutorService.schedule(() -> soundService.playTicTac(secondsBeforeEnd), solutiontime - secondsBeforeEnd, TimeUnit.SECONDS);
	}


	private void startAssignmentRunnable(final Assignment assignment, Integer solutiontime) {
		feedbackMessageController.sendStartToTeams(assignment.getName());
		assignmentHandler = scheduledExecutorService.schedule(() -> {
            feedbackMessageController.sendStopToTeams(assignment.getName());
            assignmentHandler.cancel(false);
            stopCurrentAssignment();
        }, solutiontime, TimeUnit.SECONDS);
	}
}
