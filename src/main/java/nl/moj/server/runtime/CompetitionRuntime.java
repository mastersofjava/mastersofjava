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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionRuntime {

    private final AssignmentRuntime assignmentRuntime;

    private final AssignmentService assignmentService;

    private final TeamService teamService;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final AssignmentResultRepository assignmentResultRepository;

    private final MessageService messageService;

    public static class CompetitionExecutionModel {
        @Getter
        private Competition competition;

        @Getter
        private CompetitionSession competitionSession;
        @JsonIgnore
        private List<OrderedAssignment> completedAssignments = new ArrayList<>();

        @JsonIgnore
        private AssignmentRuntime.AssignmentExecutionModel assignmentExecutionModel = new AssignmentRuntime.AssignmentExecutionModel();

        public void setAssignmentExecutionModel(AssignmentRuntime.AssignmentExecutionModel assignmentExecutionModel) {
            this.assignmentExecutionModel = assignmentExecutionModel;
        }

        public AssignmentRuntime.AssignmentExecutionModel getAssignmentExecutionModel() {
            return assignmentExecutionModel;
        }
    }
    private Map<Long, CompetitionExecutionModel> activeCompetitionsMap = new TreeMap<>();

    public Map<Long, CompetitionExecutionModel> getActiveCompetitionsMap() {
        return activeCompetitionsMap;
    }
    public Map<Long, String> getActiveCompetitionsQuickviewMap() {
        Map<Long, String> result = new TreeMap<>();
        for (CompetitionExecutionModel model : activeCompetitionsMap.values()) {
            if (model.getAssignmentExecutionModel().isRunning()) {
                result.put(model.competition.getId(), model.competition.getDisplayName());
            }
        }
        return result;
    }

    @Getter
    private CompetitionExecutionModel competitionModel = new CompetitionExecutionModel();

    public CompetitionSession getCompetitionSession() {
        return competitionModel.getCompetitionSession();
    }
    public Competition getCompetition() {
        return competitionModel.getCompetition();
    }

    private void registerCompetition(Competition competition) {
        competitionModel = new CompetitionExecutionModel();
        competitionModel.competition = competition;
        activeCompetitionsMap.put(competition.getId(), competitionModel);
    }

    public CompetitionRuntime selectCompetitionRuntimeForGameStart(Competition competition) {
        CompetitionRuntime result = new CompetitionRuntime(assignmentRuntime, assignmentService,teamService, competitionSessionRepository,assignmentResultRepository,messageService);
        if (!activeCompetitionsMap.containsKey(competition.getId())) {
            return this;
        }
        result.competitionModel = activeCompetitionsMap.get(competition.getId());
        return result;
    }
    public void startSession(Competition competition) {
        log.info("Starting new session for competition {}", competition.getName());
        registerCompetition(competition);
        competitionModel.competitionSession = competitionSessionRepository.save(createNewCompetitionSession(competition));
        restoreSession();
    }
    public List<AssignmentDescriptor> getAssignmentInfoOrderedForCompetition() {
        List<AssignmentDescriptor> notSortedList = this.getAssignmentInfo();
        return competitionModel.competition.getAssignmentsInOrder().stream()
                .map(OrderedAssignment::getAssignment)
                .map(Assignment::getName)
                .flatMap(name -> {
                    return notSortedList.stream()
                            .filter(assignmentDescriptor -> assignmentDescriptor.getName().equals(name));
                })
                .collect(Collectors.toList());
    }

    /**
     * load competition session, this also restores the competition assignments and the completed competition assignments
     * @param competition
     * @param session
     */
    public void loadSession(Competition competition, UUID session) {
        log.info("Loading session {} for competition {}", session, competition.getName());
        registerCompetition(competition);
        competitionModel.competitionSession = competitionSessionRepository.findByUuid(session);
        restoreSession();
    }
    public void changeSession(UUID session) {
        CompetitionSession competitionSession = competitionSessionRepository.findByUuid(session);
        registerCompetition(competitionSession.getCompetition());
        competitionModel.competitionSession = competitionSession;
        restoreSession();
    }
    private void restoreSession() {
        stopCurrentAssignment();
        Instant nowTime = Instant.now();
        competitionModel.competitionSession.setActive(true);
        List<CompetitionSession> sessions = competitionSessionRepository.findByCompetition(competitionModel.competition);

        for (CompetitionSession session: sessions) {
            if (session.isActive() && !session.getId().equals(competitionModel.competitionSession.getId())) {
                session.setActive(false);
                competitionSessionRepository.save(session);
            }
        }

        competitionSessionRepository.save(competitionModel.competitionSession);
        log.info("session " + competitionModel.competitionSession.getId() + " " +competitionModel.competitionSession.isActive() );
        int hour = nowTime.atZone(ZoneOffset.UTC).getHour();
        Instant maxTime = nowTime.atZone(ZoneOffset.UTC).withHour(hour-1).toInstant();
        // get the completed assignment uuids
        List<UUID> completedAssignmentList = assignmentResultRepository.findByCompetitionSession(competitionModel.competitionSession).stream()
                .filter(ar ->
                    ar.isAssignmentEnded(maxTime)
                )
                .map(ar -> ar.getAssignmentStatus().getAssignment().getUuid())
                .distinct().collect(Collectors.toList());

        // this also eagerly loads the assignments in the competition object (needed for hibernate)
        competitionModel.competition.getAssignments().forEach(oa -> {
            if (completedAssignmentList.contains(oa.getAssignment().getUuid())) {
                competitionModel.completedAssignments.add(oa);
            }
        });

        competitionModel.setAssignmentExecutionModel(assignmentRuntime.getModel());
        competitionModel.assignmentExecutionModel.setCompetitionSession(competitionModel.competitionSession);
    }

    public OrderedAssignment getCurrentAssignment() {
        if (competitionModel.assignmentExecutionModel==null) {
            return null;
        }
        if (competitionModel.assignmentExecutionModel.isRunning()) {
            return competitionModel.assignmentExecutionModel.getOrderedAssignment();
        }
        return null;
    }

    public CompetitionState getCompetitionState() {
        if (competitionModel.competitionSession != null) {
            return CompetitionState.builder()
                    .completedAssignments(competitionModel.completedAssignments)
                    .build();
        }
        return CompetitionState.builder().build();
    }

    public ActiveAssignment getActiveAssignment() {
        competitionModel.assignmentExecutionModel.setCompetitionSession(competitionModel.competitionSession);
        return competitionModel.assignmentExecutionModel.getState();
    }

    public void startAssignment(String name) {
        log.debug("stopping current assignment to start assignment '{}'", name);
        stopCurrentAssignment();
        Optional<OrderedAssignment> assignment = competitionModel.competition.getAssignments().stream()
                .filter(a -> a.getAssignment().getName().equals(name))
                .findFirst();

        if (assignment.isPresent()) {
            try {
                if (!competitionModel.completedAssignments.contains(assignment.get())) {
                    competitionModel.completedAssignments.add(assignment.get());
                    assignmentRuntime.start(assignment.get(), competitionModel);
                }
            } catch( AssignmentStartException ase ) {
                messageService.sendStartFail(name, ase.getMessage());
                log.error("Cannot start assignment '{}'.", name, ase);
            }
        } else {
            log.error("Cannot start assignment '{}' since there is no such assignment with that name", name);
        }
    }

    public void stopCurrentAssignment() {
        if (competitionModel.assignmentExecutionModel!=null && competitionModel.assignmentExecutionModel.getOrderedAssignment() != null) {
            log.info("Stopping current assignment {} uuid {}.", competitionModel.assignmentExecutionModel.getOrderedAssignment()
                            .getAssignment()
                            .getName(),
                    competitionModel.assignmentExecutionModel.getOrderedAssignment().getAssignment().getUuid());
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
        if (competitionModel.competition == null) {
            return Collections.emptyList();
        }

        return Optional.ofNullable(competitionModel.competition.getAssignmentsInOrder()).orElse(Collections.emptyList()).stream()
                .map(v -> assignmentService.getAssignmentDescriptor(v.getAssignment())
                ).sorted(Comparator.comparing(AssignmentDescriptor::getDisplayName)).collect(Collectors.toList());
    }

    public List<AssignmentFile> getTeamSolutionFiles(UUID assignment, Team team) {
        return getTeamAssignmentFiles(assignment, team).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT).collect(Collectors.toList());
    }

    private List<AssignmentFile> getTeamAssignmentFiles(UUID assignment, Team team) {
        return competitionModel.completedAssignments.stream().filter(o -> o.getAssignment().getUuid().equals(assignment)).findFirst()
                .map(orderedAssignment -> teamService.getTeamAssignmentFiles(competitionModel.competitionSession, orderedAssignment.getAssignment(), team))
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
        return competitionModel.completedAssignments.stream()
                .filter(o -> o.getAssignment().getUuid().equals(assignment))
                .findFirst()
                .map(orderedAssignment -> assignmentService.getAssignmentFiles(orderedAssignment.getAssignment()))
                .orElse(Collections.emptyList());
    }

    public List<CompetitionSession> getSessions() {
        return competitionSessionRepository.findByCompetition(competitionModel.competition);
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
