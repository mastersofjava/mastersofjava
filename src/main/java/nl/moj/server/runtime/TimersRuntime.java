package nl.moj.server.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.time.StopWatch;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.service.AssignmentStatusService;
import nl.moj.server.teams.model.Team;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimersRuntime {
    public static final long TIMESYNC_FREQUENCY = 10000L; // millis
    private static final UUID GROUP_UUID = UUID.randomUUID();

    private Map<UUID, StopWatch> timer = new HashMap<>();
    private Map<UUID, Duration> initialRemaining = new HashMap<>();
    // todo: JFALLMODE remove endtimes and base it on stopwatch
    private Map<UUID, Instant> endTimes = new HashMap<>();

    private Map<UUID, List<Future<?>>> handlers = new HashMap<>();

    private final TaskScheduler taskScheduler;
    private final MessageService messageService;
    private final AssignmentStatusService assignmentStatusService;

    public long getGroupStartTime() {
        return timer.get(GROUP_UUID).getStartTime();
    }

    public CompletableFuture<Void> startTimersForGroup(Runnable stopFunction, Duration timeRemaining,
            Assignment assignment, CompetitionSession competitionSession) {

        timer.put(GROUP_UUID, StopWatch.createStarted());
        initialRemaining.put(GROUP_UUID, timeRemaining);
        endTimes.put(GROUP_UUID, Instant.now().plus(timeRemaining));

        addHandler(GROUP_UUID, scheduleGroupStop(stopFunction, timeRemaining));
        addHandler(GROUP_UUID,
                scheduleGroupTimeSync(assignment.getAssignmentDuration(), assignment.getUuid(), competitionSession));
        return new CompletableFuture<>();
    }

    public CompletableFuture<Void> startTimerForTeam(Function<Team, Void> stopFunction, Team team,
            Duration assignmentDuration, CompetitionSession competitionSession) {
        timer.put(team.getUuid(), StopWatch.createStarted());
        initialRemaining.put(team.getUuid(), assignmentDuration);
        endTimes.put(team.getUuid(), Instant.now().plus(assignmentDuration));

        addHandler(team.getUuid(), scheduleTeamStop(stopFunction, team, assignmentDuration));
        addHandler(team.getUuid(), scheduleTeamTimeSync(team, assignmentDuration, competitionSession));

        return null;
    }

    public void addHandler(UUID teamId, Future<?> handler) {
        if (!handlers.containsKey(teamId)) {
            handlers.put(teamId, new ArrayList<>());
        }
        handlers.get(teamId).add(handler);
    }

    public Future<?> scheduleGroupStop(Runnable stopFunction, Duration timeRemaining) {
        return taskScheduler.schedule(stopFunction, secondsFromNow(timeRemaining.getSeconds()));
    }

    public Future<?> scheduleTeamStop(Function<Team, Void> stopFunction, Team team, Duration timeRemaining) {
        return taskScheduler.schedule(() -> stopFunction.apply(team), secondsFromNow(timeRemaining.getSeconds()));
    }

    public boolean isGroupRegisteredBeforeEnding(AssignmentStatus as, Instant registered) {
        // for groups we use the assignmentstate assigned end time
        log.debug(
                "checking before ending, as.getDateTimeEnd={} and expected endtime={}. Falling back to old behaviour using as.getDateTimeEnd",
                as.getDateTimeEnd(), endTimes.get(GROUP_UUID));
        return /* isRegisteredBeforeEnding(GROUP_UUID, registered) || */as.getDateTimeEnd() == null
                || (as.getDateTimeEnd() != null && registered.isBefore(as.getDateTimeEnd()));
    }

    public boolean isTeamRegisteredBeforeEnding(Team team, Instant registered) {
        return isRegisteredBeforeEnding(team.getUuid(), registered);
    }

    private boolean isRegisteredBeforeEnding(UUID id, Instant registered) {
        return endTimes.get(id) != null && endTimes.get(id).isAfter(registered);
    }

    public long getGroupSecondsRemaining(Instant registered) {
        return getSecondsRemaining(GROUP_UUID, registered);
    }

    public long getTeamSecondsRemaining(Team team, Instant registered) {
        return getSecondsRemaining(team.getUuid(), registered);

    }

    private long getSecondsRemaining(UUID id, Instant registered) {
        if (isRegisteredBeforeEnding(id, registered)) {
            return Duration.between(registered, endTimes.get(id)).toSeconds();
        } else {
            return 0;
        }
    }

    public Duration getGroupTimeRemaining() {
        return getTimeRemaining(GROUP_UUID);
    }

    public Duration getTeamTimeRemaining(Team team) {
        return getTimeRemaining(team.getUuid());
    }

    private Duration getTimeRemaining(UUID id) {
        long remaining = 0;
        if (initialRemaining.get(id) != null && timer.get(id) != null) {
            remaining = initialRemaining.get(id).getSeconds() - timer.get(id).getTime(TimeUnit.SECONDS);
            if (remaining < 0) {
                remaining = 0;
            }
        }
        return Duration.ofSeconds(remaining);
    }

    public Duration getGroupTimeElapsed(Duration duration) {
        return getTimeElapsed(GROUP_UUID, duration);
    }

    public Duration getTeamTimeElapsed(Team team, Duration duration) {
        return getTimeElapsed(team.getUuid(), duration);
    }

    private Duration getTimeElapsed(UUID id, Duration duration) {
        // todo: JFALLMODE split into client or central clock
        Duration elapsed = null;
        if (duration != null && timer.get(id) != null) {
            elapsed = Duration.ofSeconds(timer.get(id).getTime(TimeUnit.SECONDS));
            if (elapsed.compareTo(duration) > 0) {
                elapsed = duration;
            }
        }
        return elapsed;
    }

    public void clearTimers() {

        if (this.timer != null) {
            this.timer.forEach((k, v) -> {
                v.stop();
            });
            this.timer.clear();
        }

        if (this.initialRemaining != null) {
            this.initialRemaining.clear();
        }

        if (this.endTimes != null) {
            this.endTimes.clear();
        }

        if (this.handlers != null) {
            this.handlers.forEach((k, v) -> {
                v.forEach(h -> {
                    if (!h.isDone()) {
                        h.cancel(true);
                    }
                });
            });
            this.handlers.clear();
        }
    }

    public void clearTimers(Team team) {
        if (team != null) {

            if (this.timer != null && this.timer.containsKey(team.getUuid())) {
                this.timer.get(team.getUuid()).stop();
                this.timer.remove(team.getUuid());
            }

            if (this.initialRemaining != null) {
                this.initialRemaining.remove(team.getUuid());
            }

            if (this.endTimes != null) {
                this.endTimes.remove(team.getUuid());
            }

            if (this.handlers != null && handlers.containsKey(team.getUuid())) {
                this.handlers.getOrDefault(team.getUuid(), Collections.emptyList()).forEach(v -> {
                    if (!v.isDone()) {
                        v.cancel(true);
                    }
                });
                this.handlers.remove(team.getUuid());
            }
        }
    }

    public Future<?> scheduleGroupTimeSync(Duration assignmentDuration, UUID assignmentUuid,
            CompetitionSession competitionSession) {
        return taskScheduler.scheduleAtFixedRate(() -> {
            Duration remaining = getGroupTimeRemaining();
            messageService.sendGroupRemainingTime(remaining, assignmentDuration, competitionSession.getUuid());
            assignmentStatusService.updateTimeRemaining(competitionSession.getUuid(), assignmentUuid, remaining);
        }, TIMESYNC_FREQUENCY);
    }

    public Future<?> scheduleTeamTimeSync(Team team, Duration assignmentDuration,
            CompetitionSession competitionSession) {
        return taskScheduler.scheduleAtFixedRate(() -> {
            Duration remaining = getTeamTimeRemaining(team);
            messageService.sendTeamRemainingTime(team, remaining, assignmentDuration, competitionSession.getUuid());
        }, TIMESYNC_FREQUENCY);
    }

    public Instant secondsFromNow(long sec) {
        return Instant.now().plusSeconds(sec);
    }

}
