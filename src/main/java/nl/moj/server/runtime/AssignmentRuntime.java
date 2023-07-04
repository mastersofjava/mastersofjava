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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.CompetitionSession.SessionType;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.runtime.service.AssignmentStatusService;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.PathUtil;
import nl.moj.server.util.TransactionHelper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentRuntime {

    public static final long TIMESYNC_FREQUENCY = 10000L; // millis
    public static final String STOP = "STOP";
    public static final String TIMESYNC = "TIMESYNC";

    private final MojServerProperties mojServerProperties;
    private final AssignmentService assignmentService;
    private final MessageService messageService;
    private final TeamService teamService;
    private final ScoreService scoreService;
    private final TaskScheduler taskScheduler;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final AssignmentStatusService assignmentStatusService;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final AssignmentRepository assignmentRepository;

    private final TransactionHelper trx;
    private StopWatch timer;

    private Duration initialRemaining;

    @Getter
    private Assignment assignment;
    private AssignmentDescriptor assignmentDescriptor;
    private Map<String, Future<?>> handlers;

    @Getter
    private List<AssignmentFile> originalAssignmentFiles;

    @Getter
    private boolean running = false;

    private CompletableFuture<Void> done;

    // TODO this is state and should not be here
    private CompetitionSession competitionSession;

    /**
     * Starts the given {@link CompetitionAssignment} and returns
     * a Future&lt;?&gt; referencing which completes when the
     * assignment is supposed to end.
     *
     * @param sessionId    the session to start the assignment for
     * @param assignmentId the assignment to start
     * @return the {@link Future}
     */
    public CompletableFuture<Void> startCompletable(UUID sessionId, UUID assignmentId) throws AssignmentStartException {

        AssignmentStatus as = trx.requiresNew(() -> {
            clearHandlers();
            this.competitionSession = competitionSessionRepository.findByUuid(sessionId);
            this.assignment = assignmentRepository.findByUuid(assignmentId);
            this.assignmentDescriptor = assignmentService.resolveAssignmentDescriptor(assignment);

            // verify assignment
            verifyAssignment(this.assignmentDescriptor);

            // init assignment sources;
            initOriginalAssignmentFiles();

            // update assignment status
            AssignmentStatus assignmentStatus = initAssignmentStatus(competitionSession, assignment, assignmentDescriptor);

            // for group mode, we can start all teams
            if (competitionSession.getSessionType()==SessionType.GROUP) {
            	initTeamsForAssignment();
            	// update assignment status start times
            	updateTeamAssignmentStatuses();
            }

            return assignmentStatus;
        });

        // start the timers
        

        // mark assignment as running
        running = true;
        
        if (competitionSession.getSessionType()==SessionType.GROUP) {
	        done = startTimers(as.getTimeRemaining());
        }
        // send start to clients (for group mode, that will render the assignment, for single mode it will trigger the 2nd waiting screen).
        messageService.sendStartToTeams(assignment.getName(), competitionSession.getUuid().toString());

        log.info("Started assignment {}", assignment.getName());

        return done;
    }
    
//    /**
//     * Starts the assignment for a specific team. 
//     * startCompletable() is already done, there is an active assignment etc. Now we start the assignment for the team that pressed 'play' in single mode.
//     * @param sessionId    the session to start the assignment for
//     * @param assignmentId the assignment to start
//     * @return the {@link Future}
//     */
//    public CompletableFuture<Void> startSingleTeam(UUID sessionId, UUID assignmentId) throws AssignmentStartException {
//        done = startTimers(as.getTimeRemaining());
//        // send start to clients.
//        messageService.sendStartToTeam(assignment.getName(), competitionSession.getUuid().toString());
//    	
//    }


    public AssignmentStatus start(UUID sessionId, UUID assignmentId) throws AssignmentStartException {
        startCompletable(sessionId, assignmentId);
        return assignmentStatusRepository.findByCompetitionSession_UuidAndAssignment_Uuid(sessionId, assignmentId)
                .orElseThrow(() ->
                        new IllegalStateException(String.format("No assignment status found for running assignment %s in session %s", assignmentId, sessionId)));
    }

    /**
     * Stop the current assignment
     */
    public Optional<AssignmentStatus> stop() {
        if (running) {
            return trx.requiresNew(() -> {
                messageService.sendStopToTeams(assignment.getName(), competitionSession.getUuid().toString());
                teamService.getTeams().forEach(t -> {
                    TeamAssignmentStatus as = teamAssignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(assignment,
                            competitionSession, t).orElse(null);
                    if (as != null) {
                        if (as.getDateTimeCompleted() == null) {
                            as = scoreService.finalizeScore(as, assignmentDescriptor);
                            messageService.sendSubmitFeedback(as);
                        }
                        as.setDateTimeEnd(Instant.now());
                    } else {
                        log.warn("Could not finalize score for team {}@{}, no assignment status found.", t.getName(), t.getUuid());
                    }
                });
                AssignmentStatus assignmentStatus = assignmentStatusRepository.findByCompetitionSessionAndAssignment(competitionSession, assignment)
                        .orElseThrow(() -> new IllegalStateException("Missing assignment status for assignment " + assignment.getUuid()));
                assignmentStatus.setDateTimeEnd(Instant.now());
                clearHandlers();
                running = false;
                //competitionAssignment = null;
                log.info("Stopped assignment {}", assignment.getName());
                assignment = null;
                done.complete(null);
                return Optional.of(assignmentStatus);
            });
        }
        return Optional.empty();
    }

    public ActiveAssignment getState() {
    	
    	// todo: JFALLMODE split for jfall mode
    	// time remaining / time elapsed moeten per client ipv central in die mode
    	// 
    	
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

    private AssignmentStatus initAssignmentStatus(CompetitionSession session, Assignment assignment, AssignmentDescriptor ad) {
        return assignmentStatusService.createOrGet(session, assignment, ad.getDuration());
    }

    private void initTeamsForAssignment() {
        teamService.getTeams().forEach(this::initAssignmentForTeam);
    }

    public TeamAssignmentStatus initAssignmentForLateTeam(Team t) {
        TeamAssignmentStatus tas = initAssignmentForTeam(t);
        if (competitionSession.getSessionType()==SessionType.GROUP) {
        	// in group mode, a late joiner still will have the same start time as the rest of the group (otherwise they would finish later)
        	tas.setDateTimeStart(Instant.ofEpochMilli(timer.getStartTime()));
        } else {
        	// explicitly set to null since the team has to start themselves when operating in single mode
        	tas.setDateTimeStart(null);
        }
        return teamAssignmentStatusRepository.save(tas);
    }

    private TeamAssignmentStatus initAssignmentForTeam(Team t) {
        TeamAssignmentStatus tas = getOrCreateTeamAssignmentStatus(t);
        initTeamAssignmentData(t);
        return tas;
    }
    
    /** 
     * Start the current active assignment for the given team.
     * @param team
     * @return
     */
	public TeamAssignmentStatus startAssignmentForTeam(Team t) {
        TeamAssignmentStatus tas = initAssignmentForLateTeam(t);
        tas.setDateTimeStart(Instant.now());
        
        // todo: JFALLMODE  make this specific for the team
        done = startTimers(tas.getAssignment().getAssignmentDuration());
        // retrigger start assignment in frontend
        messageService.sendStartToTeams(assignment.getName(), competitionSession.getUuid().toString());
		return tas;
	}

    private void updateTeamAssignmentStatuses() {
        Instant now = Instant.now();
        teamAssignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, competitionSession)
                .forEach(as -> {
                    if (as.getDateTimeStart() == null) {
                        as.setDateTimeStart(now);
                        teamAssignmentStatusRepository.save(as);
                    }
                });
    }

    private void initTeamAssignmentData(Team team) {
        Path assignmentDirectory = teamService.getTeamAssignmentDirectory(team.getUuid(), competitionSession.getUuid(), assignment.getName());
        try {
            // create empty assignment directory
            Files.createDirectories(assignmentDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create team assignment directory " + assignmentDirectory, e);
        }
    }

    private TeamAssignmentStatus getOrCreateTeamAssignmentStatus(Team team) {
    	// todo: JFALLMODE for jfall mode do not set dateTimeStart
        return teamAssignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(assignment, competitionSession, team)
                .orElseGet(() -> {
                    TeamAssignmentStatus as = TeamAssignmentStatus.builder()
                            .assignment(assignment)
                            .competitionSession(competitionSession)
                            .uuid(UUID.randomUUID())
                            .team(team)
                            .dateTimeStart(Instant.now())
                            .build();
                    return teamAssignmentStatusRepository.save(as);
                });
    }

    private void cleanupTeamAssignmentData(Team team) {
        // delete historical submitted data.
        Path assignmentDirectory = teamService.getTeamAssignmentDirectory(team.getUuid(), competitionSession.getUuid(), assignment.getName());
        try {
            if (Files.exists(assignmentDirectory)) {
                PathUtil.delete(assignmentDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete team assignment directory " + assignmentDirectory, e);
        }
    }

    private void cleanupAssignmentStatuses() {
        teamAssignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, competitionSession)
                .forEach(teamAssignmentStatusRepository::delete);
    }

    private CompletableFuture<Void> startTimers(Duration timeRemaining) {
    	
    	// todo: JFALLMODE do not create timers for singleplayer mode

        timer = StopWatch.createStarted();
        initialRemaining = timeRemaining;

        handlers.put(STOP, scheduleStop(timeRemaining));
        handlers.put(TIMESYNC, scheduleTimeSync());
        return new CompletableFuture<>();
    }

    private Duration getTimeRemaining() {
    	// todo: JFALLMODE split into client or central clock
        long remaining = 0;
        if (initialRemaining != null && timer != null) {
            remaining = initialRemaining.getSeconds() - timer.getTime(TimeUnit.SECONDS);
            if (remaining < 0) {
                remaining = 0;
            }
        }
        return Duration.ofSeconds(remaining);
    }

    private Duration getTimeElapsed() {
    	// todo: JFALLMODE split into client or central clock
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
            this.handlers.forEach((k, v) -> {
                if (!v.isDone()) {
                    v.cancel(true);
                }
            });
        }
        this.handlers = new HashMap<>();
    }

    private Future<?> scheduleStop(Duration timeRemaining) {
        return taskScheduler.schedule(this::stop,
                secondsFromNow(timeRemaining.getSeconds()));
    }

    private Future<?> scheduleTimeSync() {
        return taskScheduler.scheduleAtFixedRate(() -> {
            Duration remaining = getTimeRemaining();
            messageService.sendRemainingTime(remaining, assignmentDescriptor.getDuration(), competitionSession.getUuid());
            assignmentStatusService.updateTimeRemaining(competitionSession.getUuid(), assignment.getUuid(), remaining);
        }, TIMESYNC_FREQUENCY);
    }

    private Instant secondsFromNow(long sec) {
        return Instant.now().plusSeconds(sec);
    }

}
