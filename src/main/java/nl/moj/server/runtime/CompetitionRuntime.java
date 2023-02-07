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
import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
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

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final CompetitionRepository competitionRepository;

    //TODO this is state we should not have
    @Getter
    private Competition competition;

    //TODO this is state we should not have
    @Getter
    private CompetitionSession competitionSession;

    //TODO this is state we should not have
    private List<CompetitionAssignment> completedAssignments;

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
        restoreSession();
        return this.competitionSession;
    }

    public void loadSession(CompetitionSession session) {
        this.competition = session.getCompetition();
        this.competitionSession = session;
        log.info("Loaded session {} for competition {}", session.getUuid(), this.competition.getName());
        restoreSession();
    }

    private void restoreSession() {
        try {
            stopCurrentAssignment();
        } catch (CompetitionServiceException e) {
            log.warn("Stopping current assignment failed during session restore, ignoring.", e);
        }
        this.completedAssignments = new ArrayList<>();

        // get the completed assignment uuids
        List<UUID> assignments = assignmentResultRepository.findByCompetitionSession(competitionSession).stream()
                .filter(ar -> ar.getAssignmentStatus().getDateTimeEnd() != null)
                .map(ar -> ar.getAssignmentStatus().getAssignment().getUuid())
                .distinct().toList();

        this.competition.getAssignments().forEach(oa -> {
            if (assignments.contains(oa.getAssignment().getUuid())) {
                this.completedAssignments.add(oa);
            }
        });
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

    @Transactional(Transactional.TxType.REQUIRED)
    public AssignmentStatus startAssignment(UUID sid, UUID id) throws CompetitionServiceException {
        // refresh competition
        competition = competitionRepository.findByUuid(competition.getUuid());

        Optional<CompetitionAssignment> ca = competition.getAssignments().stream()
                .filter(a -> a.getAssignment().getUuid().equals(id))
                .findFirst();

        if (ca.isPresent()) {
            log.debug("Stopping current assignment to start assignment '{}' '{}'", id, ca.get()
                    .getAssignment()
                    .getName());
            stopCurrentAssignment();
            try {
                AssignmentStatus as = assignmentRuntime.start(competitionSession.getUuid(), ca.get()
                        .getAssignment()
                        .getUuid());
                if (!completedAssignments.contains(ca.get())) {
                    completedAssignments.add(ca.get());
                }
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
            CompetitionAssignment ca = assignmentRuntime.getCompetitionAssignment();
            if (ca.getAssignment().getUuid().equals(id)) {
                log.info("Stopping assignment {}", id);
                competition = competitionRepository.findByUuid(competition.getUuid());
                return assignmentRuntime.stop();
            }
        }
        return assignmentStatusRepository.findByCompetitionSession_UuidAndAssignment_Uuid(sid, id).orElseThrow(
                () -> new CompetitionServiceException(String.format("Unable to stop assignment %s, not found in session %s.", id, sid)));
    }

    private void stopCurrentAssignment() throws CompetitionServiceException {
        if (assignmentRuntime.getCompetitionAssignment() != null) {
            stopAssignment(competitionSession.getUuid(), assignmentRuntime.getCompetitionAssignment().getAssignment()
                    .getUuid());
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
                .map(v -> assignmentService.resolveAssignmentDescriptor(v.getAssignment())
                ).sorted(Comparator.comparing(AssignmentDescriptor::getDisplayName)).collect(Collectors.toList());
    }

    public List<AssignmentFile> getTeamSolutionFiles(UUID assignment, Team team) {
        return getTeamAssignmentFiles(assignment, team).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT).collect(Collectors.toList());
    }

    private List<AssignmentFile> getTeamAssignmentFiles(UUID assignment, Team team) {
        return completedAssignments.stream().filter(o -> o.getAssignment().getUuid().equals(assignment)).findFirst()
                .map(orderedAssignment -> teamService.getTeamAssignmentFiles(competitionSession, orderedAssignment.getAssignment(), team.getUuid()))
                .orElse(Collections.emptyList());
    }

    public TeamAssignmentStatus handleLateSignup(Team team) {
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

    @Transactional(Transactional.TxType.REQUIRED)
    public void loadMostRecentSession() {
        competitionSessionRepository.findMostRecent().ifPresent( cs -> {
            loadSession(cs);
        });
    }
}
