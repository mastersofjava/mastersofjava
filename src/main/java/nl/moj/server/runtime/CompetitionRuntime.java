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

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionRuntime {

    private final AssignmentRuntime assignmentRuntime;

    private final AssignmentService assignmentService;

    private final TeamService teamService;

    private final TeamRepository teamRepository;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;

    private final CompetitionRepository competitionRepository;

    private final AssignmentRepository assignmentRepository;

    //TODO this is state we should not have
    @Getter
    private Competition competition;

    //TODO this is state we should not have
    @Getter
    private UUID sessionId;

    @Transactional(Transactional.TxType.REQUIRED)
    public CompetitionSession startSession(UUID id) throws CompetitionServiceException {
        Competition competition = competitionRepository.findByUuid(id);
        if (competition == null) {
            throw new CompetitionServiceException("No competition for id " + id);
        }
        return startSession(competition);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public CompetitionSession startSession(Competition competition) {
        log.info("Starting new session for session {}", competition.getName());
        CompetitionSession session = competitionSessionRepository.save(createNewCompetitionSession(competition));

        this.competition = competition;
        this.sessionId = session.getUuid();
        return session;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void continueSession(CompetitionSession session) {
        this.competition = session.getCompetition();
        this.sessionId = session.getUuid();
        log.info("Continuing session {} for competition {}", session.getUuid(), this.competition.getName());

        restoreSession(session.getUuid());
    }

    // TODO do we really need this?
    private void restoreSession(UUID sid) {
        try {
            if (getCurrentAssignment() != null) {
                stopAssignment(sid, getCurrentAssignment());
            }
        } catch (CompetitionServiceException e) {
            log.warn("Stopping current assignment failed during session restore, ignoring.", e);
        }
    }

    public ActiveAssignment getActiveAssignment() {
        return assignmentRuntime.getState();
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public AssignmentStatus startAssignment(UUID sid, UUID id) throws CompetitionServiceException {
        // refresh competition
        competition = competitionRepository.findByUuid(competition.getUuid());

        if (isAssignmentRunning(sid, id)) {
            log.info("Not starting assignment {} for session {}, already running.", id, sid);
            return assignmentStatusRepository.findByCompetitionSession_UuidAndAssignment_Uuid(sid, id)
                    .orElseThrow(() -> new CompetitionServiceException(String.format("Cannot start assignment %s, assignment not found.", id)));
        }

        Optional<CompetitionAssignment> ca = competition.getAssignments().stream()
                .filter(a -> a.getAssignment().getUuid().equals(id))
                .findFirst();
        if (ca.isPresent()) {
            if (getCurrentAssignment() != null) {
                log.debug("Stopping current assignment to start assignment '{}' '{}'", id, ca.get()
                        .getAssignment()
                        .getName());
                stopAssignment(sid, getCurrentAssignment());
            }
            try {
                AssignmentStatus as = assignmentRuntime.start(sid, ca.get()
                        .getAssignment()
                        .getUuid());
                log.debug("Assignment '{}' '{}' started.", id, ca.get().getAssignment().getName());
                return as;
            } catch (AssignmentStartException ase) {
                throw new CompetitionServiceException(String.format("Cannot start assignment %s.", id), ase);
            }
        } else {
            throw new CompetitionServiceException(String.format("Cannot start assignment %s, assignment not found.", id));
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public Optional<AssignmentStatus> stopAssignment(UUID sid, UUID id) throws CompetitionServiceException {
        if (isAssignmentRunning(sid, id)) {
            log.info("Stopping assignment {} in session {}.", id, sid);
            return assignmentRuntime.stop();
        }
        return Optional.empty();
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void resetAssignment(UUID sid, UUID id) throws CompetitionServiceException {

        CompetitionSession cs = competitionSessionRepository.findByUuid(sid);
        if (cs == null) {
            throw new CompetitionServiceException(String.format("Unable to reset assignment %s, session %s not found.", id, sid));
        }
        Assignment assignment = assignmentRepository.findByUuid(id);
        if (assignment == null) {
            throw new CompetitionServiceException(String.format("Unable to reset assignment %s, assignment %s not found.", id, sid));
        }

        // stop assignment it might be running
        stopAssignment(sid, id);

        // clean up all matching assignment statuses
        cs.getAssignmentStatuses()
                .stream()
                .filter(as -> as.getAssignment().getUuid().equals(id))
                .forEach(assignmentStatusRepository::delete);

        // clean up all team assignment statuses
        teamAssignmentStatusRepository.deleteAll(teamAssignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, cs));
    }

    public UUID getCurrentAssignment() {
        if (assignmentRuntime.getAssignment() != null) {
            return assignmentRuntime.getAssignment().getUuid();
        }
        return null;
    }

    private boolean isAssignmentRunning(UUID sid, UUID id) {
        return sid != null && sid.equals(getSessionId()) && id != null && id.equals(getCurrentAssignment());
    }

    private CompetitionSession createNewCompetitionSession(Competition competition) {
        var newCompetitionSession = new CompetitionSession();
        newCompetitionSession.setUuid(UUID.randomUUID());
        newCompetitionSession.setCompetition(competition);
        return newCompetitionSession;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public List<AssignmentFile> getTeamSolutionFiles(UUID team, UUID session, UUID assignment) {
        return getTeamAssignmentFiles(team, session, assignment).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT).collect(Collectors.toList());
    }

    private List<AssignmentFile> getTeamAssignmentFiles(UUID team, UUID session, UUID assignment) {
        return teamService.getTeamAssignmentFiles(team, session, assignment);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public TeamAssignmentStatus handleLateSignup(Team team) {
        return assignmentRuntime.initAssignmentForLateTeam(team);
    }

    public List<AssignmentFile> getSolutionFiles(UUID assignment) {
        return getAssignmentFiles(assignment).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.SOLUTION).collect(Collectors.toList());
    }

    private List<AssignmentFile> getAssignmentFiles(UUID assignment) {
        return assignmentService.getAssignmentFiles(assignment);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void loadMostRecentSession() {
        competitionSessionRepository.findMostRecent().ifPresent(this::continueSession);
    }
}
