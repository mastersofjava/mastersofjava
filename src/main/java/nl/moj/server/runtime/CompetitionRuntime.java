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
import nl.moj.server.teams.service.TeamService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionRuntime {

    private final AssignmentRuntime assignmentRuntime;

    private final AssignmentService assignmentService;

    private final TeamService teamService;

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
    private CompetitionSession competitionSession;

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
        this.competition = competition;
        this.competitionSession = competitionSessionRepository.save(createNewCompetitionSession(this.competition));
        restoreSession(this.competitionSession.getUuid());
        return this.competitionSession;
    }

    public void loadSession(CompetitionSession session) {
        this.competition = session.getCompetition();
        this.competitionSession = session;
        log.info("Loaded session {} for competition {}", session.getUuid(), this.competition.getName());
        restoreSession(session.getUuid());
    }

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
                AssignmentStatus as = assignmentRuntime.start(competitionSession.getUuid(), ca.get()
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
    public AssignmentStatus stopAssignment(UUID sid, UUID id) throws CompetitionServiceException {
        if (assignmentRuntime.isRunning()) {
            Assignment ca = assignmentRuntime.getAssignment();
            if (ca.getUuid().equals(id)) {
                log.info("Stopping assignment {}", id);
                competition = competitionRepository.findByUuid(competition.getUuid());
                return assignmentRuntime.stop();
            }
        }
        return assignmentStatusRepository.findByCompetitionSession_UuidAndAssignment_Uuid(sid, id).orElseThrow(
                () -> new CompetitionServiceException(String.format("Unable to stop assignment %s, not found in session %s.", id, sid)));
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void resetAssignment(UUID sid, UUID id) throws CompetitionServiceException {

        CompetitionSession cs = competitionSessionRepository.findByUuid(sid);
        if (cs == null) {
            throw new CompetitionServiceException(String.format("Unable to stop assignment %s, session %s not found.", id, sid));
        }
        Assignment assignment = assignmentRepository.findByUuid(id);
        if (assignment == null) {
            throw new CompetitionServiceException(String.format("Unable to stop assignment %s, assignment %s not found.", id, sid));
        }

        // clean up all matching assignment statuses
        cs.getAssignmentStatuses().stream().filter(as -> as.getAssignment().getUuid().equals(id)).forEach(assignmentStatusRepository::delete);

        // clean up all team assignment statuses
        teamAssignmentStatusRepository.deleteAll(teamAssignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, cs));
    }

    public UUID getCurrentAssignment() {
        if (assignmentRuntime.getAssignment() != null) {
            return assignmentRuntime.getAssignment().getUuid();
        }
        return null;
    }

    private CompetitionSession createNewCompetitionSession(Competition competition) {
        var newCompetitionSession = new CompetitionSession();
        newCompetitionSession.setUuid(UUID.randomUUID());
        newCompetitionSession.setCompetition(competition);
        return newCompetitionSession;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public List<AssignmentFile> getTeamSolutionFiles(UUID assignment, UUID team) {
        return getTeamAssignmentFiles(assignmentRepository.findByUuid(assignment), team).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT).collect(Collectors.toList());
    }

    private List<AssignmentFile> getTeamAssignmentFiles(Assignment assignment, UUID team) {
        return teamService.getTeamAssignmentFiles(competitionSession, assignment, team);
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
        competitionSessionRepository.findMostRecent().ifPresent(this::loadSession);
    }
}
