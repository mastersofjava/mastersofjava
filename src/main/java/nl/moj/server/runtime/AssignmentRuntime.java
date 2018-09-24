package nl.moj.server.runtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.FeedbackMessageController;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionSession;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentRuntime {

    public static final long WARNING_TIMER = 30L; // seconds
    public static final long CRITICAL_TIMER = 10L; // seconds
    public static final long TIMESYNC_FREQUENCY = 10000L; // millis
    public static final String STOP = "STOP";
    public static final String WARNING_SOUND = "WARNING_SOUND";
    public static final String CRITICAL_SOUND = "CRITICAL_SOUND";
    public static final String TIMESYNC = "TIMESYNC";

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
    private Map<String, Future<?>> handlers;

	@Getter
	private List<AssignmentFile> originalAssignmentFiles;

	@Getter
	private boolean running;

    private Map<Team,TeamStatus> teamStatuses;
	private CompetitionSession competitionSession;
    /**
	 * Starts the given {@link OrderedAssignment} and returns
	 * a Future&lt;?&gt; referencing which completes when the
	 * assignment is supposed to end.
	 * @param orderedAssignment the assignment to start.
	 * @return the {@link Future}
	 */
    @Async
	public Future<?> start(OrderedAssignment orderedAssignment, CompetitionSession competitionSession) {
		clearHandlers();
		this.competitionSession = competitionSession;
		this.orderedAssignment = orderedAssignment;
		this.assignment = orderedAssignment.getAssignment();
		this.assignmentDescriptor = assignmentService.getAssignmentDescriptor(assignment);
		this.teamStatuses = new HashMap<>();

		// init assignment sources;
		initOriginalAssignmentFiles();

		// cleanup historical assignment data
		initTeamsForAssignment();

        // play the gong
        taskScheduler.schedule(soundService::playGong, Instant.now());
		// start the timers
        Future<?> stopHandle = startTimers();

		// mark assignment as running
		running = true;

		// send start to clients.
		feedbackMessageController.sendStartToTeams(assignment.getName());

        log.info("Started assignment {}", assignment.getName());

		return stopHandle;
	}

    /**
     * Stop the current assignment
     */
	public void stop() {
		feedbackMessageController.sendStopToTeams(assignment.getName());
		if (getTimeRemaining() > 0) {
            clearHandlers();
		} else {
		    this.handlers.get(TIMESYNC).cancel(true);
        }
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
				.teamStatuses(teamStatuses)
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
			teamStatuses.put(t, TeamStatus.init(t));
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
		scoreService.initializeScoreAtStart(team, assignment,competitionSession);
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
		scoreService.removeScoresForAssignment(assignment, competitionSession);
	}

	private Future<?> startTimers() {
        timer = StopWatch.createStarted();
        Future<?> stop = scheduleStop();
        handlers.put(STOP, stop);
        handlers.put(WARNING_SOUND, scheduleAssignmentEndingNotification(assignmentDescriptor.getDuration().toSeconds() - WARNING_TIMER, WARNING_TIMER - CRITICAL_TIMER, Sound.SLOW_TIC_TAC));
        handlers.put(CRITICAL_SOUND, scheduleAssignmentEndingNotification(assignmentDescriptor.getDuration().toSeconds() - CRITICAL_TIMER, CRITICAL_TIMER, Sound.FAST_TIC_TAC));
        handlers.put(TIMESYNC, scheduleTimeSync());
        return stop;
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
			this.handlers.forEach((k, v) -> {
                v.cancel(true);
			});
		}
		this.handlers = new HashMap<>();
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
    
//    public boolean hasResubmits(String team) {
//        log.info("Team {} has used {} of {} submits", team, submits.getOrDefault(team, 0), assignmentDescriptor.getScoringRules().getMaximumResubmits());
//	    return remainingResubmits(team) > 0;
//    }

//    public int remainingResubmits(String team) {
//        return assignmentDescriptor.getScoringRules().getMaximumResubmits() - (submits.getOrDefault(team, 0));
//    }

//    public void addSubmit(String team) {
//        Integer noSubmits = submits.getOrDefault(team, 0);
//        log.info("Team {} has submitted {} times", team, noSubmits);
//        submits.put(team, ++noSubmits);
//    }

    private Date inSeconds(long sec) {
        return Date.from(LocalDateTime.now().plus(sec, ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant());
    }

    void registerAssignmentCompleted(Team team, Long timeScore, Long finalScore) {
		update(teamStatuses.get(team).toBuilder()
		.submitTime(timeScore)
		.score(finalScore)
		.build());
	}

	void registerSubmitForTeam(Team team) {
		TeamStatus s = teamStatuses.get(team);
	 	update(s.toBuilder()
			.submits(s.getSubmits()+1)
			.build()
		);
	}

	private TeamStatus update(TeamStatus status) {
		teamStatuses.put(status.getTeam(), status);
		return status;
	}


}
