package nl.moj.server.runtime;

import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionRuntime {

    private final AssignmentRuntime assignmentRuntime;

    private final AssignmentService assignmentService;

    private final TeamService teamService;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final AssignmentResultRepository assignmentResultRepository;

    @Getter
    private Competition competition;

    @Getter
    private CompetitionSession competitionSession;

    private List<OrderedAssignment> completedAssignments;

    public void startSession(Competition competition) {
        log.info("Starting new session for competition {}", competition.getName());
        this.competition = competition;
        this.competitionSession = competitionSessionRepository.save(createNewCompetitionSession(competition));
        restoreSession();
    }

    public void loadSession(Competition competition, UUID session) {
        log.info("Loading session {} for competition {}", session, competition.getName());
        this.competition = competition;
        this.competitionSession = competitionSessionRepository.findByUuid(session);
        restoreSession();
    }

    private void restoreSession() {
        stopCurrentAssignment();
        this.completedAssignments = new ArrayList<>();

        // get the completed assignment uuids
        List<UUID> assignments = assignmentResultRepository.findByCompetitionSession(competitionSession).stream()
                .filter(ar -> ar.getAssignmentStatus().getDateTimeEnd() != null)
                .map(ar -> ar.getAssignmentStatus().getAssignment().getUuid())
                .distinct().collect(Collectors.toList());

        this.competition.getAssignments().forEach(oa -> {
            if (assignments.contains(oa.getAssignment().getUuid())) {
                this.completedAssignments.add(oa);
            }
        });
    }

    public OrderedAssignment getCurrentAssignment() {
        if (assignmentRuntime.isRunning()) {
            return assignmentRuntime.getOrderedAssignment();
        }
        return null;
    }

    public CompetitionState getCompetitionState() {
        if (competitionSession != null) {
            return CompetitionState.builder()
                    .completedAssignments(completedAssignments)
                    .build();
        }
        return CompetitionState.builder().build();
    }

    public ActiveAssignment getActiveAssignment() {
        return assignmentRuntime.getState();
    }

    public void startAssignment(String name) {
        log.debug("stopping current assignment to start assignment '{}'", name);
        stopCurrentAssignment();
        Optional<OrderedAssignment> assignment = competition.getAssignments().stream()
                .filter(a -> a.getAssignment().getName().equals(name))
                .findFirst();

        if (assignment.isPresent()) {
            assignmentRuntime.start(assignment.get(), competitionSession);
            if (!completedAssignments.contains(assignment.get())) {
                completedAssignments.add(assignment.get());
            }
        } else {
            log.error("Cannot start assignment '{}' since there is no such assignment with that name", name);
        }
    }

    public void stopCurrentAssignment() {
        if (assignmentRuntime.getOrderedAssignment() != null) {
            log.info("Stopping current assignment {} uuid {}.", assignmentRuntime.getOrderedAssignment()
                            .getAssignment()
                            .getName(),
                    assignmentRuntime.getOrderedAssignment().getAssignment().getUuid());
            assignmentRuntime.stop();
        }
    }

    private CompetitionSession createNewCompetitionSession(Competition competition) {
        var newCompetitionSession = new CompetitionSession();
        newCompetitionSession.setUuid(UUID.randomUUID());
        newCompetitionSession.setCompetition(competition);
        return newCompetitionSession;
    }

    public List<AssignmentDescriptor> getAssignmentInfo() {
        if (competition == null) {
            return Collections.emptyList();
        }

        return Optional.ofNullable(competition.getAssignments()).orElse(Collections.emptyList()).stream()
                .map(v -> assignmentService.getAssignmentDescriptor(v.getAssignment())
                ).sorted(Comparator.comparing(AssignmentDescriptor::getDisplayName)).collect(Collectors.toList());
    }

    public List<AssignmentFile> getTeamSolutionFiles(UUID assignment, Team team) {
        return getTeamAssignmentFiles(assignment, team).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.SOLUTION).collect(Collectors.toList());
    }

    private List<AssignmentFile> getTeamAssignmentFiles(UUID assignment, Team team) {
        return completedAssignments.stream().filter(o -> o.getAssignment().getUuid().equals(assignment)).findFirst()
                .map(orderedAssignment -> teamService.getTeamAssignmentFiles(competitionSession, orderedAssignment.getAssignment(), team))
                .orElse(Collections.emptyList());
    }

    public AssignmentStatus handleLateSignup(Team team) {
        return assignmentRuntime.initAssignmentForLateTeam(team);
    }

    public List<AssignmentFile> getSolutionFiles(UUID assignment) {
        return getAssignmentFiles(assignment).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.SOLUTION).collect(Collectors.toList());
    }

    private List<AssignmentFile> getAssignmentFiles(UUID assignment) {
        return completedAssignments.stream()
                .filter(o -> o.getAssignment().getUuid().equals(assignment))
                .findFirst()
                .map(orderedAssignment -> assignmentService.getAssignmentFiles(orderedAssignment.getAssignment()))
                .orElse(Collections.emptyList());
    }

    public List<CompetitionSession> getSessions() {
        return competitionSessionRepository.findByCompetition(competition);
    }

    public void loadMostRecentSession(Competition competition) {
        CompetitionSession session = competitionSessionRepository.findByCompetition(competition)
                .stream()
                .max(Comparator.comparing(CompetitionSession::getId))
                .orElse(null);

        if (session == null) {
            startSession(competition);
        } else {
            loadSession(competition, session.getUuid());
        }
    }
}
