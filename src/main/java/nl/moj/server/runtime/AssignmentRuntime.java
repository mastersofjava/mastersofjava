package nl.moj.server.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.runtime.model.TeamStatus;
import nl.moj.server.sound.Sound;
import nl.moj.server.sound.SoundService;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.PathUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentRuntime {

    public static final long WARNING_TIMER = 40L; // seconds
    public static final long CRITICAL_TIMER = 25L; // seconds
    public static final long TIMESYNC_FREQUENCY = 2000L; // millis

    private final AssignmentService assignmentService;
    private final FeedbackMessageController feedbackMessageController;
    private final TeamService teamService;
    private final ScoreService scoreService;
    private final SoundService soundService;
    private final TaskScheduler taskScheduler;
    private StopWatch timer;

    @Getter
	private OrderedAssignment orderedAssignment;
	private Assignment assignment;
	private AssignmentDescriptor assignmentDescriptor;
	private List<Future<?>> handlers;

	@Getter
	private List<AssignmentFile> originalAssignmentFiles;

	@Getter
	private boolean running;

	private List<TeamStatus> finishedTeams;

    @Async
    /**
     * Start an assignment
     * @param orderedAssignment The assignment to start
     */
	public void start(OrderedAssignment orderedAssignment) {
		clearHandlers();
		this.orderedAssignment = orderedAssignment;
		this.assignment = orderedAssignment.getAssignment();
		this.assignmentDescriptor = assignmentService.getAssignmentDescriptor(assignment);
		this.finishedTeams = new ArrayList<>();

		// init assignment sources;
		initOriginalAssignmentFiles();

		// cleanup historical assignment data
		initTeamsForAssignment();

		// start the timers
		startTimers();

		// mark assignment as running
		running = true;

		// send start to clients.
		feedbackMessageController.sendStartToTeams(assignment.getName());

		// play the gong
        taskScheduler.schedule(() ->
                soundService.playGong(),
                inSeconds(0)
        );
        log.info("Started assignment {}", assignment.getName());
	}

    /**
     * Stop the current assignment
     */
	public void stop() {
		feedbackMessageController.sendStopToTeams(assignment.getName());
		clearHandlers();
		running = false;
        log.info("Stopped assignment {}", assignment.getName());
	}

// TODO this should probably not be here
	public List<AssignmentFile> getTeamAssignmentFiles(Team team) {
		List<AssignmentFile> teamFiles = new ArrayList<>();
		Path teamAssignmentBase = resolveTeamAssignmentBaseDirectory(team);
		originalAssignmentFiles.forEach(f -> {
			Path resolvedFile = teamAssignmentBase.resolve(f.getFile());
			if (resolvedFile.toFile().exists() && Files.isReadable(resolvedFile)) {
				teamFiles.add(f.toBuilder()
						.content(readPathContent(resolvedFile))
						.build());
			} else {
				teamFiles.add(f.toBuilder().build());
			}
		});
		return teamFiles;
	}

	public AssignmentState getState() {
		return AssignmentState.builder()
				.timeRemaining(getTimeRemaining())
				.assignmentDescriptor(assignmentDescriptor)
				.assignmentFiles(originalAssignmentFiles)
				.running(running)
				.finishedTeams(finishedTeams)
				.build();
	}

	private String readPathContent(Path p) {
		try {
			return IOUtils.toString(Files.newInputStream(p, StandardOpenOption.READ), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read assignment file " + p, e);
		}
	}

	private void initOriginalAssignmentFiles() {
		originalAssignmentFiles = new ArrayList<>();
		Path assignmentBase = assignmentDescriptor.getDirectory();
		assignmentDescriptor.getAssignmentFiles().getEditable().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(assignmentBase.resolve(p), AssignmentFileType.EDIT, false));
		});
		assignmentDescriptor.getAssignmentFiles().getReadonly().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(assignmentBase.resolve(p), AssignmentFileType.READONLY, true));
		});
		assignmentDescriptor.getAssignmentFiles().getTests().forEach(p -> {
			originalAssignmentFiles.add(convertToAssignmentFile(assignmentBase.resolve(p), AssignmentFileType.TEST, true));
		});
		originalAssignmentFiles.add(convertToAssignmentFile(assignmentBase.resolve(assignmentDescriptor.getAssignmentFiles().getAssignment()), AssignmentFileType.TASK, true));
	}

	private AssignmentFile convertToAssignmentFile(Path p, AssignmentFileType type, boolean readOnly) {
		return AssignmentFile.builder()
				.assignment(assignment.getName())
				.content(readPathContent(p))
				.file(p)
				.filename(p.getFileName().toString())
				.name(p.getFileName().toString().substring(0, p.getFileName().toString().indexOf(".")))
				.fileType(type)
				.readOnly(readOnly)
				.build();
	}

	private void initTeamsForAssignment() {

		cleanupTeamScores();
		teamService.getTeams().forEach(t -> {
			cleanupTeamAssignmentData(t);
			initTeamScore(t);
			initTeamAssignmentData(t);
		});
	}

	private void initTeamAssignmentData(Team team) {
		Path assignmentDirectory = resolveTeamAssignmentBaseDirectory(team);
		try {
			// create empty assignment directory
			Files.createDirectories(assignmentDirectory);

			// copy assignment files
//			Path src = assignmentDescriptor.getDirectory();
//
//			assignmentDescriptor.getAssignmentFiles().getSources().forEach( p -> {
//				try {
//					Files.copy(src.resolve(p), assignmentDirectory.resolve(p));
//				} catch (IOException e) {
//					throw new RuntimeException("Unable to copy assignment sources for team " + team, e);
//				}
//			});
		} catch (IOException e) {
			throw new RuntimeException("Unable to delete team assignment directory " + assignmentDirectory, e);
		}

	}

	private Path resolveTeamAssignmentBaseDirectory(Team team) {
		return teamService.getTeamDirectory(team).resolve(assignment.getName());
	}

	private void initTeamScore(Team team) {
		scoreService.initializeScoreAtStart(team.getName(), assignment.getName());
	}

	private void cleanupTeamAssignmentData(Team team) {
		// delete historical submitted data.
		Path assignmentDirectory = resolveTeamAssignmentBaseDirectory(team);
		try {
			if( Files.exists(assignmentDirectory)) {
				PathUtil.delete(assignmentDirectory);
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to delete team assignment directory " + assignmentDirectory, e);
		}
	}

	private void cleanupTeamScores() {
		scoreService.removeScoresForAssignment(assignmentDescriptor.getName());
	}

    public void startTimers() {
        clearHandlers();
        timer = StopWatch.createStarted();
        handlers.add(scheduleStop());
        handlers.add(scheduleAssignmentEndingNotification(assignmentDescriptor.getDuration().toSeconds() - WARNING_TIMER, WARNING_TIMER - CRITICAL_TIMER, Sound.SLOW_TIC_TAC));
        handlers.add(scheduleAssignmentEndingNotification(assignmentDescriptor.getDuration().toSeconds() - CRITICAL_TIMER, CRITICAL_TIMER, Sound.FAST_TIC_TAC));
        handlers.add(scheduleTimeSync());
    }

    private Long getTimeRemaining() {
		long remaining = 0;
        if (assignmentDescriptor != null && timer != null) {
            remaining = assignmentDescriptor.getDuration().getSeconds() - timer.getTime(TimeUnit.SECONDS);
			if (remaining < 0) {
				remaining = 0;
			}
		}
		return remaining;
	}

	private void clearHandlers() {
		if (this.handlers != null) {
			this.handlers.forEach(h -> h.cancel(true));
		}
		this.handlers = new ArrayList<>();
	}

    @Async
    public Future<?> scheduleStop() {
        return taskScheduler.schedule(this::stop, inSeconds(assignmentDescriptor.getDuration().getSeconds()));
	}

	@Async
    public Future<?> scheduleAssignmentEndingNotification(long start, long duration, Sound sound) {
        return taskScheduler.schedule(() -> soundService.play(sound, duration), inSeconds(start));
	}

    @Async
    public Future<?> scheduleTimeSync() {
        return taskScheduler.scheduleAtFixedRate(
                () -> {
                    feedbackMessageController.sendRemainingTime(getTimeRemaining(), assignmentDescriptor.getDuration().getSeconds());
                },
                TIMESYNC_FREQUENCY
        );
	}

	public void addFinishedTeam(TeamStatus team) {
		finishedTeams.add(team);
	}

	private Date inSeconds(long sec) {
        return Date.from(LocalDateTime.now().plus(sec, ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant());

    }
}
