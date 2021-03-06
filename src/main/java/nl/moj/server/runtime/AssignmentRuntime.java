/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.sound.Sound;
import nl.moj.server.sound.SoundService;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.PathUtil;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final MojServerProperties mojServerProperties;
    private final AssignmentService assignmentService;
    private final MessageService messageService;
    private final TeamService teamService;
    private final ScoreService scoreService;
    private final SoundService soundService;
    private final TaskScheduler taskScheduler;
    private final AssignmentStatusRepository assignmentStatusRepository;

    // TODO refactor so we do not need to use ApplicationContext to find a self reference
    @Autowired
    private ApplicationContext ctx;

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

    private CompetitionSession competitionSession;

    /**
     * Starts the given {@link OrderedAssignment} and returns
     * a Future&lt;?&gt; referencing which completes when the
     * assignment is supposed to end.
     *
     * @param orderedAssignment the assignment to start.
     * @return the {@link Future}
     */
    @Transactional
    public Future<?> start(OrderedAssignment orderedAssignment, CompetitionSession competitionSession) throws AssignmentStartException {
        clearHandlers();
        this.competitionSession = competitionSession;
        this.orderedAssignment = orderedAssignment;
        this.assignment = orderedAssignment.getAssignment();
        this.assignmentDescriptor = assignmentService.getAssignmentDescriptor(assignment);

        // verify assignment
        verifyAssignment(this.assignmentDescriptor);

        // init assignment sources;
        initOriginalAssignmentFiles();

        initTeamsForAssignment();

        // play the gong
        taskScheduler.schedule(soundService::playGong, Instant.now());
        // start the timers
        Future<?> stopHandle = startTimers();

        // update assignment status start times
        updateTeamAssignmentStatuses();

        // mark assignment as running
        running = true;

        // send start to clients.
        messageService.sendStartToTeams(assignment.getName());

        log.info("Started assignment {}", assignment.getName());

        return stopHandle;
    }

    /**
     * Stop the current assignment
     */
    @Transactional
    public void stop() {
        messageService.sendStopToTeams(assignment.getName());
        teamService.getTeams().forEach(t -> {
            ActiveAssignment state = getState();
            AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(),
                    state.getCompetitionSession(), t);
            if (as != null) {
                if (as.getDateTimeEnd() == null) {
                    as = scoreService.finalizeScore(as, state);
                    AssignmentResult ar = as.getAssignmentResult();
                    messageService.sendSubmitFeedback(t, SubmitResult.builder()
                            .success(false)
                            .remainingSubmits(0)
                            .score(ar.getFinalScore())
                            .build());
                }
            } else {
                log.warn("Could not finalize score for team {}@{}, no assignment status found.", t.getName(), t.getUuid());
            }
        });

        if (getTimeRemaining() > 0) {
            clearHandlers();
        } else {
            try {
                this.handlers.get(TIMESYNC).cancel(true);
            } catch (NullPointerException e) {
                log.debug("assignment stopped without being started, not canceling timesync handler since it doesn't exist");
            }
        }
        running = false;
        orderedAssignment = null;
        log.info("Stopped assignment {}", assignment.getName());
    }

    public ActiveAssignment getState() {
        return ActiveAssignment.builder()
                .competitionSession(competitionSession)
                .assignment(assignment)
                .timeRemaining(getTimeRemaining())
                .timeElapsed(getTimeElapsed())
                .assignmentDescriptor(assignmentDescriptor)
                .assignmentFiles(originalAssignmentFiles)
                .running(running)
                .build();
    }

    private void verifyAssignment(AssignmentDescriptor ad) throws AssignmentStartException {
        // verify we have a correct runtime available.
        try {
            mojServerProperties.getLanguages().getJavaVersion(ad.getJavaVersion());
        } catch (IllegalArgumentException iae) {
            throw new AssignmentStartException("Cannot start assignment " + ad.getName() + ", requested Java runtime version " + ad
                    .getJavaVersion() + " not available.", iae);
        }
    }

    private void initOriginalAssignmentFiles() {
        try {
            originalAssignmentFiles = assignmentService.getAssignmentFiles(assignment);
        } catch (Exception e) {
            // log exception here since it may get swallowed by async calls
            log.error("Unable to parse assignment files for assignment {}: {}", assignmentDescriptor.getDisplayName(), e
                    .getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void initTeamsForAssignment() {
        cleanupAssignmentStatuses();
        teamService.getTeams().forEach(this::initAssignmentForTeam);
    }

    public AssignmentStatus initAssignmentForLateTeam(Team t) {
        AssignmentStatus as = initAssignmentForTeam(t);
        as.setDateTimeStart(Instant.ofEpochMilli(timer.getStartTime()));
        return assignmentStatusRepository.save(as);
    }

    private AssignmentStatus initAssignmentForTeam(Team t) {
        cleanupTeamAssignmentData(t);
        AssignmentStatus as = initAssignmentStatus(t);
        initTeamScore(as);
        initTeamAssignmentData(t);
        return as;
    }

    private void updateTeamAssignmentStatuses() {
        assignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, competitionSession).forEach(as -> {
            as.setDateTimeStart(Instant.ofEpochMilli(timer.getStartTime()));
            assignmentStatusRepository.save(as);
        });
    }

    private void initTeamAssignmentData(Team team) {
        Path assignmentDirectory = teamService.getTeamAssignmentDirectory(competitionSession, team, assignment);
        try {
            // create empty assignment directory
            Files.createDirectories(assignmentDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create team assignment directory " + assignmentDirectory, e);
        }
    }

    private AssignmentStatus initAssignmentStatus(Team team) {
        Duration assignmentDuration = assignmentDescriptor.getDuration();
        AssignmentStatus as = AssignmentStatus.builder()
                .assignment(assignment)
                .competitionSession(competitionSession)
                .uuid(UUID.randomUUID())
                .assignmentDuration(assignmentDuration)
                .team(team)
                .build();
        return assignmentStatusRepository.save(as);
    }


    private void initTeamScore(AssignmentStatus as) {
        scoreService.initializeScoreAtStart(as);
    }

    private void cleanupTeamAssignmentData(Team team) {
        // delete historical submitted data.
        Path assignmentDirectory = teamService.getTeamAssignmentDirectory(competitionSession, team, assignment);
        try {
            if (Files.exists(assignmentDirectory)) {
                PathUtil.delete(assignmentDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete team assignment directory " + assignmentDirectory, e);
        }
    }

    private void cleanupAssignmentStatuses() {
        assignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, competitionSession)
                .forEach(assignmentStatusRepository::delete);
    }

    private Future<?> startTimers() {
        timer = StopWatch.createStarted();
        Future<?> stop = scheduleStop();
        handlers.put(STOP, stop);
        handlers.put(WARNING_SOUND, scheduleAssignmentEndingNotification(assignmentDescriptor.getDuration()
                .toSeconds() - WARNING_TIMER, WARNING_TIMER - CRITICAL_TIMER, Sound.SLOW_TIC_TAC));
        handlers.put(CRITICAL_SOUND, scheduleAssignmentEndingNotification(assignmentDescriptor.getDuration()
                .toSeconds() - CRITICAL_TIMER, CRITICAL_TIMER, Sound.FAST_TIC_TAC));
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

    private Duration getTimeElapsed() {
        Duration elapsed = null;
        if (assignmentDescriptor != null && timer != null) {
            elapsed = Duration.ofSeconds(timer.getTime(TimeUnit.SECONDS));
            if (elapsed.compareTo(assignmentDescriptor.getDuration()) > 0) {
                elapsed = assignmentDescriptor.getDuration();
            }
        }
        return elapsed;
    }

    private void clearHandlers() {
        if (this.handlers != null) {
            this.handlers.forEach((k, v) -> v.cancel(true));
        }
        this.handlers = new HashMap<>();
    }

    @Async
    public Future<?> scheduleStop() {
        AssignmentRuntime ar = getSelfReference();
        return taskScheduler.schedule(ar::stop, inSeconds(assignmentDescriptor.getDuration().getSeconds()));
    }

    @Async
    public Future<?> scheduleAssignmentEndingNotification(long start, long duration, Sound sound) {
        return taskScheduler.schedule(() -> soundService.play(sound, duration), inSeconds(start));
    }

    @Async
    public Future<?> scheduleTimeSync() {
        return taskScheduler.scheduleAtFixedRate(() -> messageService.sendRemainingTime(getTimeRemaining(),
                assignmentDescriptor.getDuration().getSeconds()), TIMESYNC_FREQUENCY);
    }

    private Date inSeconds(long sec) {
        return Date.from(LocalDateTime.now().plus(sec, ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant());
    }

    private AssignmentRuntime getSelfReference() {
        return ctx.getBean(AssignmentRuntime.class);
    }
}
