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
import nl.moj.server.runtime.model.AssignmentFileType;
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
import org.springframework.util.Assert;

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
   // private ApplicationContext ctx;


    public static class AssignmentExecutionModel  {
        @Getter
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

        @Getter
        private boolean paused;
        private CompetitionSession competitionSession;
        private Instant startTime;

        public ActiveAssignment getState() {
            return ActiveAssignment.builder()
                    .competitionSession(this.competitionSession)
                    .assignment(this.assignment)
                    .timeRemaining(getTimeRemaining())
                    .timeElapsed(getTimeElapsed())
                    .assignmentDescriptor(this.assignmentDescriptor)
                    .assignmentFiles(this.originalAssignmentFiles)
                    .running(this.running)
                    .build();
        }
        private Long getTimeRemaining() {
            long remaining = 0;
            if (this.assignmentDescriptor != null && this.timer != null) {
                remaining = this.assignmentDescriptor.getDuration().getSeconds() - this.timer.getTime(TimeUnit.SECONDS);
                if (remaining < 0) {
                    remaining = 0;
                }
            }
            return remaining;
        }

        private Duration getTimeElapsed() {
            Duration elapsed = null;
            if (this.assignmentDescriptor != null && this.timer != null) {
                elapsed = Duration.ofSeconds(this.timer.getTime(TimeUnit.SECONDS));
                if (elapsed.compareTo(this.assignmentDescriptor.getDuration()) > 0) {
                    elapsed = this.assignmentDescriptor.getDuration();
                }
            }
            return elapsed;
        }

        public void setCompetitionSession(CompetitionSession competitionSession) {
            this.competitionSession = competitionSession;
        }
    }

    public OrderedAssignment getOrderedAssignment() {
        return model.orderedAssignment;
    }


    public boolean isPaused() {
        return model.paused;
    }

    public boolean isRunning() {
        return model.running;
    }

    private AssignmentExecutionModel model = new AssignmentExecutionModel();

    /**
     * Starts the given {@link OrderedAssignment} and returns
     * a Future&lt;?&gt; referencing which completes when the
     * assignment is supposed to end.
     *
     * @param orderedAssignment the assignment to start.
     * @return the {@link Future}
     */
    public Future<?> start(OrderedAssignment orderedAssignment, CompetitionRuntime.CompetitionExecutionModel competitionExecutionModel) throws AssignmentStartException {
        model = new AssignmentExecutionModel();
        Future<?> result = start(orderedAssignment, competitionExecutionModel.getCompetitionSession());
        competitionExecutionModel.setAssignmentExecutionModel(model);
        return result;
    }

    @Transactional
    public Future<?> start(OrderedAssignment orderedAssignment, CompetitionSession competitionSession) throws AssignmentStartException {
        clearHandlers();
        model.startTime = Instant.now();
        model.competitionSession = competitionSession;
        model.orderedAssignment = orderedAssignment;
        model.assignment = orderedAssignment.getAssignment();
        model.assignmentDescriptor = assignmentService.getAssignmentDescriptor(model.assignment);

        // verify assignment
        verifyAssignment(model.assignmentDescriptor);

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
        model.running = true;

        // send start to clients.
        messageService.sendStartToTeams(model.assignment.getName(), model.competitionSession.getUuid().toString());

        log.info("Started assignment {}", model.assignment.getName());

        return stopHandle;
    }

    public AssignmentExecutionModel getModel() {
        return model;
    }

    public void pauseResume() {
        if (!model.paused) {
            if (model.timer.isStarted()) {
                model.timer.suspend();
            }
            model.paused = true;
        } else {
            if (model.timer.isSuspended()) {
                model.timer.resume();
            }

            model.paused = false;
        }
    }

    /**
     * Stop the current assignment
     */
    @Transactional
    public void stop() {
        messageService.sendStopToTeams(model.competitionSession.getUuid().toString(), model.assignment.getName());
        teamService.getTeams().forEach(t -> {
            ActiveAssignment state = model.getState();
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

        if (model.getTimeRemaining() > 0) {
            clearHandlers();
        } else {
            try {
                model.handlers.get(TIMESYNC).cancel(true);
            } catch (NullPointerException e) {
                log.debug("assignment stopped without being started, not canceling timesync handler since it doesn't exist");
            }
        }
        model.running = false;
        model.orderedAssignment = null;
        log.info("Stopped assignment {}", model.assignment.getName());
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
            if (model.assignment!=null) {
                model.originalAssignmentFiles = assignmentService.getAssignmentFiles(model.assignment);
            }
        } catch (Exception e) {
            // log exception here since it may get swallowed by async calls
            log.error("Unable to parse assignment files for assignment {}: {}", model.assignmentDescriptor.getDisplayName(), e
                    .getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    public void reloadOriginalAssignmentFiles() {
        assignmentService.clearSmallFileStorageInMemory();
        initOriginalAssignmentFiles();
    }

    private void initTeamsForAssignment() {
        cleanupAssignmentStatuses();
        teamService.getTeams().forEach(this::initAssignmentForTeam);
    }

    public AssignmentStatus initAssignmentForLateTeam(Team t) {
        AssignmentStatus as = initAssignmentForTeam(t);
        as.setDateTimeStart(Instant.ofEpochMilli(model.timer.getStartTime()));
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
        assignmentStatusRepository.findByAssignmentAndCompetitionSession(model.assignment, model.competitionSession).forEach(as -> {
            as.setDateTimeStart(Instant.ofEpochMilli(model.timer.getStartTime()));
            assignmentStatusRepository.save(as);
        });
    }

    private void initTeamAssignmentData(Team team) {
        Path assignmentDirectory = teamService.getTeamAssignmentDirectory(model.competitionSession, team, model.assignment);
        try {
            // create empty assignment directory
            Files.createDirectories(assignmentDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create team assignment directory " + assignmentDirectory, e);
        }
    }

    private AssignmentStatus initAssignmentStatus(Team team) {
        Duration assignmentDuration = model.assignmentDescriptor.getDuration();

        AssignmentStatus as = AssignmentStatus.builder()
                .assignment(model.assignment)
                .competitionSession(model.competitionSession)
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
        Path assignmentDirectory = teamService.getTeamAssignmentDirectory(model.competitionSession, team, model.assignment);
        try {
            if (Files.exists(assignmentDirectory)) {
                PathUtil.delete(assignmentDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete team assignment directory " + assignmentDirectory, e);
        }
    }

    private void cleanupAssignmentStatuses() {
        assignmentStatusRepository.findByAssignmentAndCompetitionSession(model.assignment, model.competitionSession)
                .forEach(assignmentStatusRepository::delete);
    }

    private Future<?> startTimers() {
        model.timer = StopWatch.createStarted();
        Future<?> stop = scheduleStop();
        model.handlers.put(STOP, stop);
        model.handlers.put(WARNING_SOUND, scheduleAssignmentEndingNotification(model.assignmentDescriptor.getDuration()
                .toSeconds() - WARNING_TIMER, WARNING_TIMER - CRITICAL_TIMER, Sound.SLOW_TIC_TAC));
        model.handlers.put(CRITICAL_SOUND, scheduleAssignmentEndingNotification(model.assignmentDescriptor.getDuration()
                .toSeconds() - CRITICAL_TIMER, CRITICAL_TIMER, Sound.FAST_TIC_TAC));
        model.handlers.put(TIMESYNC, scheduleTimeSync());
        return stop;
    }



    private void clearHandlers() {
        if (model.handlers != null) {
            model.handlers.forEach((k, v) -> v.cancel(true));
        }
        model.handlers = new HashMap<>();
    }

    @Async
    public Future<?> scheduleStop() {
        final AssignmentRuntime ar = this;
        return taskScheduler.schedule(ar::stop, inSeconds(model.assignmentDescriptor.getDuration().getSeconds()));
    }

    @Async
    public Future<?> scheduleAssignmentEndingNotification(long start, long duration, Sound sound) {
        return taskScheduler.schedule(() -> soundService.play(sound, duration), inSeconds(start));
    }

    @Async
    public Future<?> scheduleTimeSync() {
        return taskScheduler.scheduleAtFixedRate(() -> messageService.sendRemainingTime(model.getTimeRemaining(),
                model.assignmentDescriptor.getDuration().getSeconds(), model.isPaused(), model.competitionSession.getUuid().toString()), TIMESYNC_FREQUENCY);
    }

    private Date inSeconds(long sec) {
        return Date.from(LocalDateTime.now().plus(sec, ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant());
    }
    @Deprecated
    private AssignmentRuntime getSelfReference() {
        return this;
    }
}
