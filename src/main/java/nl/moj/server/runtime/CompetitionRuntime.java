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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
@Slf4j
// TODO this needs to be cleaned up!!
public class CompetitionRuntime {

    private final AssignmentRuntime assignmentRuntime;

    private final AssignmentService assignmentService;

    private final TeamService teamService;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final AssignmentResultRepository assignmentResultRepository;

    private final AssignmentRepository assignmentRepository;

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

        public AssignmentRuntime.AssignmentExecutionModel getAssignmentExecutionModel() {
            return assignmentExecutionModel;
        }

        public boolean isRunning() {
            return assignmentExecutionModel.isRunning();
        }
        public int getAmountOfCompletedAssignments() {
            return completedAssignments.size();
        }
        public boolean isNewAssignmentInCompetition(OrderedAssignment orderedAssignment) {
            return !completedAssignments.contains(orderedAssignment);
        }

        public String getRunningAssignmentName() {
            if (assignmentExecutionModel.getOrderedAssignment()==null||assignmentExecutionModel.getOrderedAssignment().getAssignment()==null) {
                return "none";
            }
            return assignmentExecutionModel.getOrderedAssignment().getAssignment().getName();
        }
        public String getTimer() {
            if (assignmentExecutionModel.getTimer()==null) {
                return "[no timer]";
            }
            return "[start " +new Date(assignmentExecutionModel.getTimer().getStartTime()) + ", left:" + assignmentExecutionModel.getTimeRemaining()+ "]";
        }
    }
    private Map<Long, CompetitionExecutionModel> activeCompetitionsMap = new TreeMap<>();

    public Map<Long, CompetitionExecutionModel> getActiveCompetitionsMap() {
        return activeCompetitionsMap;
    }
    public Map<Long, String> getRunningCompetitionsQuickviewMap() {
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


    private void registerCurrentAdminCompetition(Competition competition) {
        if (activeCompetitionsMap.containsKey(competition.getId())) {
            competitionModel = activeCompetitionsMap.get(competition.getId());
            competitionModel.competition = competition;
            return;
        }
        competitionModel = new CompetitionExecutionModel();
        competitionModel.competition = competition;
        activeCompetitionsMap.put(competition.getId(), competitionModel);
    }

    public boolean isActiveCompetition(Competition competition) {
        return activeCompetitionsMap.get(competition.getId())!=null;
    }
    public Competition selectCompetitionByUUID(UUID uuid) {
        Competition result = null;
        for (CompetitionExecutionModel model: activeCompetitionsMap.values()) {
            if (model.getCompetition().getUuid().equals(uuid) || model.getCompetitionSession().getUuid().equals(uuid)) {
                result = model.getCompetition();
            }
        }
        return result;
    }
    public CompetitionRuntime selectCompetitionRuntimeForGameStart(Competition competition) {
        CompetitionRuntime result = new CompetitionRuntime(assignmentRuntime, assignmentService,teamService, competitionSessionRepository,assignmentResultRepository,assignmentRepository,messageService);
        if (activeCompetitionsMap.get(competition.getId())==null) {
            return this;
        }
        result.competitionModel = activeCompetitionsMap.get(competition.getId());
        return result;
    }
    public void startSession(Competition competition) {
        log.info("Starting new session for competition {}", competition.getName());
        registerCurrentAdminCompetition(competition);
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
        registerCurrentAdminCompetition(competition);
        competitionModel.competitionSession = competitionSessionRepository.findByUuid(session);
        restoreSession();
    }
    public void changeSession(UUID session) {
        log.info("changeSession " +session );
        CompetitionSession competitionSession = competitionSessionRepository.findByUuid(session);
        registerCurrentAdminCompetition(competitionSession.getCompetition());
        competitionModel.competitionSession = competitionSession;
        restoreSession();
    }
    @Transactional
    public void restoreSession() {
        //stopCurrentAssignment();
        Instant nowTime = Instant.now();
        competitionModel.competitionSession.setAvailable(true);
        List<CompetitionSession> sessions = competitionSessionRepository.findByCompetition(competitionModel.competition);

        for (CompetitionSession session: sessions) {
            if (session.isAvailable() && !session.getId().equals(competitionModel.competitionSession.getId())) {
                session.setAvailable(false);
                competitionSessionRepository.save(session);
            }
        }

        competitionSessionRepository.save(competitionModel.competitionSession);
        log.info("restoreSession " + competitionModel.competitionSession.getId() + " " +competitionModel.competitionSession.isRunning()  + " " +competitionModel.competitionSession.getTimeLeft() + " " +competitionModel.competition.getShortName());
        Instant maxTime = nowTime.atZone(ZoneOffset.UTC).minusHours(1).toInstant();
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

        competitionModel.assignmentExecutionModel.setCompetitionSession(competitionModel.competitionSession);
    }

    public OrderedAssignment getCurrentRunningAssignment() {
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

    public OrderedAssignment determineNextAssignmentIfAny() {
        List<OrderedAssignment> orderedAssignmentList = competitionModel.competition.getAssignmentsInOrder();

        for (OrderedAssignment assigment: orderedAssignmentList) {
            if (!competitionModel.completedAssignments.contains(assigment)) {
                return assigment;
            }
        }
        return null;
    }

    public ActiveAssignment getActiveAssignment() {
        if (getActiveCompetitionsMap().containsKey(competitionModel.competition.getId())) {
            competitionModel = getActiveCompetitionsMap().get(competitionModel.competition.getId());
        }
        if (competitionModel.assignmentExecutionModel.getOrderedAssignment()==null) {
            return null;
        }
        return competitionModel.assignmentExecutionModel.getState();
    }
    public void startAssignment(String name) {
        startAssignment(name, -1);
    }
    public void startAssignment(String name, long timeLeft) {

        Optional<OrderedAssignment> assignment = competitionModel.competition.getAssignments().stream()
                .filter(a -> a.getAssignment().getName().equals(name))
                .findFirst();
        if (!assignment.isPresent()) {
            log.error("Cannot start assignment '{}' since there is no such assignment with that name", name);
            return;
        }
        boolean isNew = competitionModel.isNewAssignmentInCompetition(assignment.get()) || competitionModel.getCompetitionSession().isRunning();
        if (!isNew) {
            log.error("Cannot start assignment '{}' because already started/completed in competition {}.", name , competitionModel.competition.getName());
            return;
        }
        if (isWithAssignmentRunning() && !competitionModel.getCompetitionSession().isRunning() ) {
            log.debug("stopping current assignment to start assignment '{}'", name);
            stopCurrentSession();
        }

        try {
            log.info("startAssignment name {}, c {}", name , competitionModel.competition.getName());
            assignmentRuntime.start(assignment.get(), competitionModel);
            competitionModel.completedAssignments.add(assignment.get());
            getCompetitionSession().setRunning(true);
            getCompetitionSession().setAssignmentName(name);
            getCompetitionSession().setDateTimeStart(Instant.now());
            getCompetitionSession().setDateTimeLastUpdate(Instant.now());
            if (timeLeft>0) {
                assignmentRuntime.getModel().resetTimer();
                getCompetitionSession().setTimeLeft(timeLeft);
            }
            competitionSessionRepository.save(getCompetitionSession());
        } catch( AssignmentStartException ase ) {
            competitionModel.completedAssignments.remove(assignment.get());
            messageService.sendStartFail(name, ase.getMessage());
            log.error("Cannot start assignment '{}'.", name);
        }
    }
    private boolean isWithAssignmentRunning() {
        return competitionModel.assignmentExecutionModel!=null && competitionModel.assignmentExecutionModel.getOrderedAssignment() != null;
    }
    public void stopCurrentSession() {
        if (isWithAssignmentRunning()) {
            log.info("Stopping current assignment {} uuid {}.", competitionModel.assignmentExecutionModel.getOrderedAssignment()
                            .getAssignment()
                            .getName(),
                    competitionModel.assignmentExecutionModel.getOrderedAssignment().getAssignment().getUuid());
            assignmentRuntime.stop();
            getCompetitionSession().setRunning(false);
            getCompetitionSession().setDateTimeLastUpdate(Instant.now());
            competitionSessionRepository.save(getCompetitionSession());
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
                .map(v -> assignmentService.resolveAssignmentDescriptor(v.getAssignment())
                ).sorted(Comparator.comparing(AssignmentDescriptor::getDisplayName)).collect(Collectors.toList());
    }

    public List<AssignmentFile> getTeamSolutionFiles(UUID assignment, Team team) {
        return getTeamAssignmentFiles(assignment, team).stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT).collect(Collectors.toList());
    }

    private List<AssignmentFile> getTeamAssignmentFiles(UUID assignment, Team team) {
        return competitionModel.completedAssignments.stream().filter(o -> o.getAssignment().getUuid().equals(assignment)).findFirst()
                .map(orderedAssignment -> teamService.getTeamAssignmentFiles(competitionModel.competitionSession, orderedAssignment.getAssignment(), team.getUuid()))
                .orElse(Collections.emptyList());
    }
    public AssignmentStatus handleLateSignup(Team team) {
        return competitionModel.getAssignmentExecutionModel().initAssignmentForLateTeam(team);
    }
    public AssignmentStatus handleLateSignup(Team team, UUID uuidInput, String nameInput) {
        UUID uuid   = competitionModel.getAssignmentExecutionModel().getCompetitionSession().getUuid();
        String name = competitionModel.getAssignmentExecutionModel().getOrderedAssignment().getAssignment().getName();

        Assert.isTrue(name.equals(nameInput),"name not valid: " + nameInput + ", expected " +name);
        Assert.isTrue(uuid.equals(uuidInput),"uuid not valid: " + uuidInput + ", expected " +uuid);
        return competitionModel.getAssignmentExecutionModel().initAssignmentForLateTeam(team);
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

    public ActiveAssignment getActiveAssignment(Team team,SourceMessage sourceMessage) {
        ActiveAssignment activeAssignment = getActiveAssignmentValidate(team,sourceMessage);
        Assert.isTrue(activeAssignment!=null, "activeAssignment missing");
        Assert.isTrue(activeAssignment.getCompetitionSession()!=null, "CompetitionSession missing");
        return activeAssignment;
    }

    // TODO this is bizarre logic and needs to be cleaned up
    private ActiveAssignment getActiveAssignmentValidate(Team team, SourceMessage sourceMessage) {
        if (sourceMessage==null || sourceMessage.getUuid() == null) {
            return getActiveAssignment();
        }
        UUID uuid = UUID.fromString(sourceMessage.getUuid());
        Competition competition = selectCompetitionByUUID(uuid);

        ActiveAssignment activeAssignment = null;
        if (competition!=null) {
            activeAssignment = selectCompetitionRuntimeForGameStart(competition).getActiveAssignment();
        }
        if (activeAssignment!=null && sourceMessage.getAssignmentName().equals(activeAssignment.getAssignment().getName())) {
            return activeAssignment;
        }
        // TODO wtf is going on here?
        CompetitionSession competitionSession = getCompetitionSession();
        boolean isGlobalUuid = uuid.equals(competitionSession.getUuid());
        if (!isGlobalUuid) {
            competitionSession = competitionSessionRepository.findByUuid(uuid);
        }
        Assignment assignment = assignmentRepository.findByName( sourceMessage.getAssignmentName() );
        List<AssignmentFile> fileList = assignmentService.getAssignmentFiles(assignment);
        AssignmentDescriptor assignmentDescriptor = assignmentService.resolveAssignmentDescriptor(assignment);

        long timeLeft = Long.parseLong(sourceMessage.getTimeLeft());
        long timeElapsed = assignmentDescriptor.getDuration().toSeconds()-timeLeft;
        boolean isWithSubmitValidation = competitionSession.isRunning();

        // TODO @kaben No clue what this does and how to test this!
        if (!isWithSubmitValidation)  {
            // after competition completion, we still allow arriving submits for 5 minutes (because of performance reasons)
            long maxSubmitDelayAfterFinish = competitionSession.getDateTimeLastUpdate().plusSeconds(5*60).toEpochMilli();
            long timeDelta = sourceMessage.getArrivalTime() - maxSubmitDelayAfterFinish;

            if (isWithDelayedSubmitValidation(competitionSession, sourceMessage)) {
                isWithSubmitValidation = true;
                log.info("Team " + team.getName() + " submitted before max submit time ( milliseconds left : "+timeDelta+", attempt will be used).");
            } else {
                log.info("Warning: Team " + team.getName() + " submitted far after max submit time ( milliseconds too late: "+timeDelta+", attempt will be ignored).");
            }
        }
        activeAssignment  = ActiveAssignment.builder()
                .competitionSession(competitionSession)
                .assignment(assignment)
                .timeElapsed(Duration.ofSeconds(timeElapsed))
                .timeRemaining(timeLeft)
                .running(isWithSubmitValidation)
                .assignmentDescriptor(assignmentDescriptor)
                .assignmentFiles(fileList).build();// individual user

        return activeAssignment;
    }
    public boolean isWithDelayedSubmitValidation(CompetitionSession competitionSession, SourceMessage message) {
        long maxSubmitDelayAfterFinish = competitionSession.getDateTimeLastUpdate().plusSeconds(5*60).toEpochMilli();
        long timeDelta = message.getArrivalTime() - maxSubmitDelayAfterFinish;
        return timeDelta<0;
    }
}
