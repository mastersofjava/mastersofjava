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
import nl.moj.server.sound.SoundService;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.PathUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentRuntime {

	private final AssignmentService assignmentService;
	private final ScheduledExecutorService scheduledExecutorService;
	private final FeedbackMessageController feedbackMessageController;
	private final TeamService teamService;
	private final ScoreService scoreService;
	private final SoundService soundService;

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
	private CompetitionSession competitionSession;

	/**
	 * Starts the given {@link OrderedAssignment} and returns
	 * a Future&lt;?&gt; referencing which completes when the
	 * assignment is supposed to end.
	 * @param orderedAssignment the assignment to start.
	 * @return the {@link Future}
	 */
	public Future<?> start(OrderedAssignment orderedAssignment, CompetitionSession competitionSession) {
		clearHandlers();
		this.competitionSession = competitionSession;
		this.orderedAssignment = orderedAssignment;
		this.assignment = orderedAssignment.getAssignment();
		this.assignmentDescriptor = assignmentService.getAssignmentDescriptor(assignment);
		this.finishedTeams = new ArrayList<>();

		// init assignment sources;
		initOriginalAssignmentFiles();

		// cleanup historical assignment data
		initTeamsForAssignment();

		// play the gong
		scheduledExecutorService.schedule(soundService::playGong, 0, TimeUnit.SECONDS);

		// TODO this could be nicer in the sense we should bundle all futures.
		// start the timers
		Future<?> stopHandle = startTimers();

		// mark assignment as running
		running = true;

		// send start to clients.
		feedbackMessageController.sendStartToTeams(assignment.getName());

		log.info("Started assignment {}", assignment.getName());

		return stopHandle;
	}

	public void stop() {
		feedbackMessageController.sendStopToTeams(assignment.getName());
		if( getTimeRemaining() > 0 ) {
			clearHandlers();
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
		scoreService.removeScoresForAssignment(assignment);
	}

	private Future<?> startTimers() {
		timer = StopWatch.createStarted();
		Future<?> stop = scheduleStop();
		handlers.add(stop);
		handlers.add(scheduleAssignmentEndingNotification(120));
		handlers.add(scheduleAssignmentEndingNotification(60));
		handlers.add(scheduleTimeSync());
		return stop;
	}

	private Long getTimeRemaining() {
		long remaining = 0;
		if( assignmentDescriptor != null && timer != null ) {
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

	private Future<?> scheduleStop() {
		return scheduledExecutorService.schedule(this::stop, assignmentDescriptor.getDuration().getSeconds(), TimeUnit.SECONDS);
	}

	private Future<?> scheduleAssignmentEndingNotification(int secondsBeforeEnd) {
		return scheduledExecutorService.schedule(() -> soundService.playTicTac(secondsBeforeEnd), assignmentDescriptor.getDuration().getSeconds() - secondsBeforeEnd, TimeUnit.SECONDS);
	}

	private Future<?> scheduleTimeSync() {
		return scheduledExecutorService.scheduleAtFixedRate(() -> {
			feedbackMessageController.sendRemainingTime(getTimeRemaining(), assignmentDescriptor.getDuration().getSeconds());
		}, 1, 10, TimeUnit.SECONDS);
	}

	public void addFinishedTeam(TeamStatus team) {
		finishedTeams.add(team);
	}
}
