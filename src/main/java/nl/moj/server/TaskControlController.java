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
package nl.moj.server;

import javax.annotation.security.RolesAllowed;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.authorization.Role;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionCleaningService;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.rankings.service.RankingsService;
import nl.moj.server.runtime.AssignmentRuntime;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class TaskControlController {

    private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

    private final MojServerProperties mojServerProperties;

    private final CompetitionRuntime competition;

    private final AssignmentService assignmentService;

    private final AssignmentRepository assignmentRepository;

    private final CompetitionRepository competitionRepository;

    private final TeamRepository teamRepository;

    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;

    private final AssignmentRuntime assignmentRuntime;

    //private final GamemasterTableComponents gamemasterTableComponents;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final CompetitionCleaningService competitionCleaningService;

    private final RankingsService rankingsService;

    private final CompetitionService competitionService;

    private final SessionRegistry sessionRegistry;

    private final UserService userService;

    @ModelAttribute(name = "locationList")
    public List<File> locationList() {
        return competitionService.locationList();
    }

    private List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @MessageMapping("/control/starttask")
    @SendToUser("/queue/controlfeedback")
    public String startTask(TaskMessage message) {
        competition.startAssignment(message.getTaskName());
        return "started assignment '" + message.getTaskName() + "', reloading page";
    }

    @MessageMapping("/control/stoptask")
    @SendToUser("/queue/controlfeedback")
    public String stopTask(TaskMessage message) {
        competition.stopCurrentAssignment();
        ActiveAssignment state = competition.getActiveAssignment();
//        boolean isWithNewAssignment = message!=null && !StringUtils.isEmpty(message.taskName);
//        if (isWithNewAssignment) {
//            long timeLeft = assignmentRuntime.getModel().getState().getAssignmentDescriptor().getDuration().toSeconds();
//            competition.startAssignment(message.taskName,timeLeft);
//        } else {
//            competition.getCompetitionSession().setTimeLeft(null);
//            competition.getCompetitionSession().setDateTimeLastUpdate(null);
//            competition.getCompetitionSession().setRunning(false);
//            competitionSessionRepository.save(competition.getCompetitionSession());
//        }
//        String name = "default";
//        if(state!=null && state.getAssignment()!=null) {
//            name = state.getAssignment().getName();
//        }
        return "stopped assignment '" + state.getAssignment().getName() + "' running, reloading page";
    }

    @MessageMapping("/control/clearCompetition")
    @SendToUser("/queue/controlfeedback")
    public String doClearCompetition() {
        log.warn("clearCompetition entered");
        // TODO implement
//        competition.stopCurrentSession();
//        competitionCleaningService.doCleanComplete(competition.getCompetitionSession());
//        competition.getCompetitionState().getCompletedAssignments().clear();
        return "Not Implemented";
    }

    @MessageMapping("/control/pauseResume")
    @SendToUser("/queue/controlfeedback")
    public String pauseResume() {
        log.warn("pauseResume entered");
        // TODO implement
//        ActiveAssignment state = competition.getActiveAssignment();
//        if (state==null|| state.getAssignment()==null) {
//            return "no active assignment";
//        }
//        String name = state.getAssignment().getName();
//        competition.getCompetitionModel().getAssignmentExecutionModel().pauseResume();
//        if (competition.getCompetitionModel().getAssignmentExecutionModel().isPaused()) {
//            return "assignment '"+name+"' paused, reloading page";
//        } else {
//            return "assignment '"+name+"' running, reloading page";
//        }
        return "Not Implemented";
    }


    @MessageMapping("/control/restartAssignment")
    @SendToUser("/queue/controlfeedback")
    public String restartAssignment(TaskMessage message) {
        log.warn("restartAssignment entered = {} ", message.taskName);
//        ActiveAssignment state = competition.getActiveAssignment();
//        boolean isStopCurrentAssignment=state!=null && state.getAssignment()!=null && state.getAssignment().getName().equals(message.taskName);
//
//        if (isStopCurrentAssignment) {
//            competition.stopCurrentSession();
//        }
//        Assignment assignment = assignmentRepository.findByName(message.taskName);
//        List<TeamAssignmentStatus> ready4deletionList = teamAssignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, competition.getCompetitionSession());
//        if (!ready4deletionList.isEmpty()) {
//            for (TeamAssignmentStatus status: ready4deletionList) {
//                teamAssignmentStatusRepository.deleteById(status.getId());// correct cleaning: first delete all status items, afterwards delete all results
//            }
//        }
//        List<OrderedAssignment> operatableList = new ArrayList<>(competition.getCompetitionState().getCompletedAssignments());
//        for (OrderedAssignment orderedAssignment: operatableList) {
//            if (orderedAssignment.getAssignment().getName().equals(assignment.getName())) {
//                competition.getCompetitionState().getCompletedAssignments().remove(orderedAssignment);
//            }
//        }
//        boolean isWithRestartDirectly = !StringUtils.isEmpty(message.getValue());
//
//        if (isWithRestartDirectly) {
//            long timeLeft = assignmentService.resolveAssignmentDescriptor(assignment).getDuration().toSeconds();
//            competition.getCompetitionSession().setRunning(true);
//            competition.startAssignment(message.getValue(),timeLeft);// start fresh
//            return "Assignment restarted directly: " + message.taskName + ", reload page";
//        } else {
//            competition.getCompetitionSession().setTimeLeft(null);
//            competition.getCompetitionSession().setDateTimeLastUpdate(null);
//            competition.getCompetitionSession().setRunning(false);
//            competitionSessionRepository.save(competition.getCompetitionSession());
//        }
        return "Not Implemented";
    }


    @MessageMapping("/control/competitionSaveName")
    public void doCompetitionSaveName(TaskControlController.TaskMessage message) throws JsonProcessingException {
//        log.warn("doCompetitionSaveName = {} ", message.getValue() );
//
//        UUID uuid = UUID.fromString(message.getUuid());
//        Competition competitionToUpdate =  competitionRepository.findByUuid(uuid);
//        String[] parts = competitionToUpdate.getName().split("\\|");
//        String input = message.getValue();
//        if (parts.length>=2) {
//            input = message.getValue() + "|" +parts[1];
//        }
//        competitionToUpdate.setName(input );
//        competitionRepository.save(competitionToUpdate);
        // TODO remove?
    }

    @MessageMapping("/control/competitionDelete")
    @SendToUser("/queue/controlfeedback")
    // @Transactional
    public String doDeleteCompetition(TaskControlController.TaskMessage message) {
//        boolean isUpdateCurrentCompetition =  message.getUuid().equals(competition.getCompetition().getUuid().toString());
//        log.info("deleteCompetition isCurrentCompetition {} ", isUpdateCurrentCompetition);
//
//        try {
//            long startAmount = competitionRepository.count();
//            Competition competitionToClean = competitionRepository.findByUuid(UUID.fromString(message.getUuid()));
//            List<CompetitionSession> sessionsToDelete = competitionSessionRepository.findByCompetition(competitionToClean);
//            log.info("sessionsToDelete {}, c {}", ""+ sessionsToDelete.size(), ""+ competitionToClean.getName());
//
//            for (CompetitionSession session: sessionsToDelete) {
//                competitionCleaningService.doCleanComplete(session);
//                competitionSessionRepository.delete(session);
//                // remove from active competitions
//                competition.getActiveCompetitionsMap().remove(competitionToClean.getId());
//            }
//            if (startAmount>1) {
//
//                competitionRepository.delete(competitionToClean);
//                if (isUpdateCurrentCompetition) {
//                    List<Competition> list = competitionRepository.findAll();
//
//                    competition.loadMostRecentSession(list.get(0));
//                }
//            } else {
//                competition.startSession(competitionToClean); // always guarantee at least one competition.
//            }
//
//
//        } catch (Exception ex) {
//            log.info("Error during deletion", ex);
//            return "Error during deletion";
//        }
//        return "Deleted competition, now reloading page";
        return "Delete competition not implemented.";
    }

    @MessageMapping("/control/competitionToggleAvailability")
    public void doCompetitionToggleAvailability(TaskMessage message) throws JsonProcessingException {
//        CompetitionSession item = competitionSessionRepository.findByUuid(UUID.fromString(message.getUuid()));
//        item.setAvailable(Boolean.valueOf(message.value));
//        competitionSessionRepository.save(item);
        // TODO remove
    }

    @MessageMapping("/control/competitionCreateNew")
    @SendToUser("/queue/controlfeedback")
    public String doCompetitionCreateNew(TaskMessage message) throws JsonProcessingException {
        log.info("doCompetitionCreateNew value {} ", message.getValue());
//        if (StringUtils.isBlank(message.value)|| !message.value.contains("|")) {
//            return "Please provide a valid name. ";
//        }
//        Competition newCompetition = new Competition();
//        newCompetition.setUuid(UUID.randomUUID());
//        newCompetition.setName(message.getValue());
//        Competition registeredCompetition = competitionRepository.save(newCompetition);
//        registeredCompetition.setAssignments(assignmentRepository.findAll()
//                .stream()
//                .map(competitionService.createOrderedAssignments(registeredCompetition))
//                .collect(Collectors.toList()));
//        Competition playableCompetition = competitionRepository.save(registeredCompetition);
//
//        competition.startSession(playableCompetition);
//        return "New competition created, reloading page";
        // TODO remove?
        return "Create competition not implemented";
    }

    @MessageMapping("/control/updateTeamStatus")
    @SendToUser("/queue/controlfeedback")
    public String doUpdateUserStatus(TaskControlController.TaskMessage message) throws JsonProcessingException {
        log.info("updateUserStatus value {} ", message.getValue());
        if (StringUtils.isBlank(message.uuid)) {
            return "Please provide valid input.";
        }
        Team team = teamRepository.findByUuid(UUID.fromString(message.getUuid()));
        if (team == null) {
            return "Team already deleted.";
        }
        UserStatusUpdate updateType = UserStatusUpdate.getEnum(message.getValue());
        if (!updateType.isAllowedToPlay) {
            team.setIndication(message.value);
        } else {
            team.setCompany(message.value);
        }
        teamRepository.save(team);
        return updateType.value + " team '" + team.getName() + "'";
    }

    private enum UserStatusUpdate {
        DEFAULT("Updated team", true),
        DISQUALIFY("Disqualified team", false),
        ARCHIVE("Archived team", false);
        private String value;
        private boolean isAllowedToPlay;

        UserStatusUpdate(String value, boolean isAllowedToPlay) {
            this.value = value;
            this.isAllowedToPlay = isAllowedToPlay;
        }

        public static UserStatusUpdate getEnum(String value) {
            UserStatusUpdate type = DEFAULT;
            if (DISQUALIFY.name().equals(value) || ARCHIVE.name().equals(value)) {
                type = UserStatusUpdate.valueOf(value);
            }
            return type;
        }
    }

    @MessageMapping("/control/assignment/scan")
    @SendToUser("/queue/controlfeedback")
    public String scanAssignments() {
        try {
            List<Assignment> assignments = assignmentService.updateAssignments();
            if (assignments.isEmpty()) {
                return "No assignments found in folder " + mojServerProperties.getAssignmentRepo() + ".";
            }
            log.info("Found {} assignments in folder {}.", assignments.size(), mojServerProperties.getAssignmentRepo());

            return "Assignments scanned from location " + mojServerProperties.getAssignmentRepo() + " (" + assignments.size() + "), reloading to show them.";
        } catch (Exception e) {
            log.error("Scanning assignments failed.", e);
            return e.getMessage();
        }
    }

    private void startCompetitionWithFreshAssignments(String name) {
        Competition resetCompetition = competition.getCompetition();

        resetCompetition.setName(name);
        // wipe previous assignments
        resetCompetition.setAssignments(new ArrayList<>());

        Competition startCompetition = competitionRepository.save(resetCompetition);

        Assert.isTrue(startCompetition.getAssignments().isEmpty(), "competition should have no assignments");
        // re-add updated assignments
        startCompetition.setAssignments(assignmentRepository.findAll()
                .stream()
                .map(competitionService.createOrderedAssignments(startCompetition))
                .collect(Collectors.toList()));
        startCompetition = competitionRepository.save(startCompetition);

        competition.loadSession(startCompetition, competition.getCompetitionSession().getUuid());
    }

    /*
    private class AdminPageStatus {
        private List<String> roles;
        private String selectedYearLabel;
        private boolean isWithAdminRole;
        private boolean isWithSecretCurrentYear;
        private User user;
        private AdminPageStatus(Authentication principal,User user) {
            this.user = user;
            this.roles = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
            this.selectedYearLabel = competitionService.getSelectedYearLabel();
            this.isWithAdminRole = roles.contains(Role.ADMIN);
            this.isWithSecretCurrentYear = selectedYearLabel.contains("2020");

        }
        private void insertPageDefaults(Model model) {
            Map<Long,String> activeCompetitions = competition.getRunningCompetitionsQuickviewMap();

            model.addAttribute("isWithAdminRole", this.isWithAdminRole);
            model.addAttribute("timeLeft", 0);
            model.addAttribute("time", 0);
            model.addAttribute("running", false);
            model.addAttribute("runningSelectedCompetition", HttpUtil.hasParam("running"));
            model.addAttribute("clockStyle", "active");

            model.addAttribute("assignmentDetailCanvas", "(U moet eerst de opdrachten inladen)");
            model.addAttribute("gameDetailCanvas", "(U moet eerst de opdrachten inladen)");
            model.addAttribute("opdrachtConfiguraties","(U moet eerst de opdrachten inladen)");
            model.addAttribute("isWithConfigurableTestScore",false);
            model.addAttribute("isWithHiddenTests",false);
            model.addAttribute("activeTeamDetailCanvas", "(U moet eerst de gebruikers aanmaken)");
            model.addAttribute("teamDetailCanvas", "(U moet eerst de gebruikers aanmaken)");
            model.addAttribute("repositoryLocation", mojServerProperties.getAssignmentRepo().toFile());
            model.addAttribute("selectedYearLabel", "");
            model.addAttribute("competitionName", competition.getCompetition().getShortName());
            model.addAttribute("isWithCompetitionStarted",false);
            model.addAttribute("nrOfUsersOnline", sessionRegistry.getAllPrincipals().size());
            model.addAttribute("currentUserName", user.getName());

            model.addAttribute("nrOfRunningCompetitions", activeCompetitions.size());

            model.addAttribute("currentAssignment", "-");
            if (HttpUtil.hasParam("running")) {
                String assignment = HttpUtil.getParam("running","");
                if (!assignment.isEmpty()) {
                    model.addAttribute("currentAssignment", assignment);
                }
            }
        }
        private void insertGamestatus(Model model) {
            log.info("insertGamestatus " +competition.getCurrentRunningAssignment()+ " " +competition.getCompetitionSession().isRunning());
            boolean isSwitch = competition.getCurrentRunningAssignment() == null;
            if (isSwitch && competition.getCompetitionSession().isRunning()) {
                log.info("CompetitionSession.refresh " + competition.getCompetitionSession().getAssignmentName());
                competition.startAssignment(competition.getCompetitionSession().getAssignmentName());
            }
            ActiveAssignment state = competition.getActiveAssignment();
            Assert.isTrue(state!=null,"incorrect status, view logs");
            CompetitionRuntime.CompetitionExecutionModel competitionModel = competition.selectCompetitionRuntimeForGameStart(competition.getCompetition()).getCompetitionModel();

            model.addAttribute("timeLeft", state.getSecondsRemaining());
            model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addAttribute("running", state.isRunning());
            boolean isRunningSelected = competitionModel.isRunning()|| HttpUtil.hasParam("running");

            model.addAttribute("runningSelectedCompetition", isRunningSelected);
            model.addAttribute("clockStyle", (competitionModel.getAssignmentExecutionModel().isPaused()?"disabled":"active"));
            model.addAttribute("currentAssignment", state.getAssignmentDescriptor().getName());
        }
        private void insertAssignmentInfo(Model model) {
            List<AssignmentDescriptor> assignmentDescriptorList = competition.getAssignmentInfoOrderedForCompetition();

            boolean isWithAssignmentsLoaded = !assignmentDescriptorList.isEmpty();
            List<String> completedAssignments = new ArrayList<>();
            if (isWithAssignmentsLoaded) {
                List<GamemasterTableComponents.DtoAssignmentState> statusList = gamemasterTableComponents.createAssignmentStatusList();
                completedAssignments.addAll(gamemasterTableComponents.createCompletedAssigmentList(statusList));
                String assignmentDetailCanvas = gamemasterTableComponents.toSimpleBootstrapTable(assignmentDescriptorList);
                String gameDetailCanvas = gamemasterTableComponents.toSimpleBootstrapTableForAssignmentStatus(statusList);
                model.addAttribute("isWithCompetitionStarted",gameDetailCanvas.contains("STARTED"));
                model.addAttribute("isWithConfigurableTestScore",assignmentDetailCanvas.contains("(*2)"));
                model.addAttribute("isWithHiddenTests",assignmentDetailCanvas.contains("(*1)"));
                model.addAttribute("assignmentDetailCanvas",  assignmentDetailCanvas);
                model.addAttribute("gameDetailCanvas", gameDetailCanvas);
                model.addAttribute("opdrachtConfiguraties", gamemasterTableComponents.toSimpleBootstrapTablesForFileDetails(assignmentDescriptorList));
            }
            model.addAttribute("assignments",assignmentDescriptorList);
            model.addAttribute("isWithAssignmentsLoaded", isWithAssignmentsLoaded);
            model.addAttribute("completedAssignments", completedAssignments);
        }

        private void validateRoleAuthorization() {
            Assert.isTrue(roles.contains(Role.ADMIN)||roles.contains(Role.GAME_MASTER),"not authorized");
            Assert.isTrue(!isWithSecretCurrentYear||isWithAdminRole,"Gamemasters are not authorized to see secret current year assignments");
        }
        private boolean isDuringCompetitionAssignment() {
            return competition.getCompetitionSession().isRunning();
        }
        private void insertCompetitionInfo(Model model) {
            List<CompetitionSession> sessions = competitionSessionRepository.findAll();
            if (sessions.isEmpty()) {
                competition.startSession(competition.getCompetition());
                sessions.add(competition.getCompetitionSession());
            }
            List<Team> teams = getAllTeams();
            if (!teams.isEmpty() && this.isWithAdminRole) {
                List<Ranking> rankings = rankingsService.getRankings(competition.getCompetitionSession(), competitionService.getSelectedYearValue());
                model.addAttribute("teamDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTableForTeams(teams, true, rankings));
                if (teams.size()>1) {
                    model.addAttribute("activeTeamDetailCanvas",  gamemasterTableComponents.toSimpleBootstrapTableForTeams(teams, false, rankings));
                }
            }
            model.addAttribute("teams", teams);
            if (this.isWithAdminRole) {
                model.addAttribute("repositoryLocation", competitionService.getSelectedLocation());
                model.addAttribute("selectedYearLabel", this.selectedYearLabel);
            }
            model.addAttribute("sessions", sessions);
            model.addAttribute("keycloackUrl", mojServerProperties.getAuthServerUrl());
            model.addAttribute("setting_registration_disabled", true);
            model.addAttribute("sessionDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTableForSessions());

        }
    }*/

    //-- rest endpoints

    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    @PostMapping(value = "/api/competition", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addCompetition(@RequestBody AddCompetition addCompetition) {
        try {
            competitionService.createCompetition(addCompetition.getName(), addCompetition.getAssignments());
            return ResponseEntity.noContent().build();
        } catch (CompetitionServiceException cse) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    @PostMapping(value = "/api/competition/session", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,String>> startSession(@RequestBody StartSession startSession) {
        try {
            CompetitionSession session = competitionService.startSession(comp);
            return ResponseEntity.ok(Map.of("name",session.getCompetition().getName(), "id", session.getUuid().toString()));
        } catch (CompetitionServiceException cse) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Value
    @Builder
    @Jacksonized
    public static class AddCompetition {
        String name;
        List<UUID> assignments;
    }

    @Value
    @Builder
    @Jacksonized
    public static class StartSession {
        UUID uuid;
    }

    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    @GetMapping("/control")
    public String taskControl(Model model, Authentication principal) {
        // TODO maybe move this creat or update stuff to a filter.
        User user = userService.createOrUpdate(principal);

        model.addAttribute("assignments", allAssignments());
        model.addAttribute("competitions", allCompetitions());
        model.addAttribute("competition", toCompetitionSessionVO(competition));

        //model.addAttribute("running", state.isRunning());
        model.addAttribute("clockStyle", "active");

//        if (competition.getActiveAssignment().isRunning()) {
//            model.addAttribute("timeLeft", state.getTimeRemaining().toSeconds());
//            model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
//        }


        return "control";
    }

    private List<AssignmentVO> allAssignments() {
        return assignmentRepository.findAll(Sort.by("collection", "name")).stream()
                .map(this::toAssignmentVO).toList();
    }

    private List<CompetitionVO> allCompetitions() {
        return competitionRepository.findAll(Sort.by("name")).stream()
                .map(this::toCompetitionVO).toList();
    }

    private CompetitionVO toCompetitionVO(Competition co) {
        return CompetitionVO.builder()
                .uuid(co.getUuid())
                .name(co.getName())
                .assignments(co.getAssignments().stream().map(ca -> toAssignmentVO(ca.getAssignment())).toList())
                .build();
    }

    private CompetitionSessionVO toCompetitionSessionVO(CompetitionRuntime runtime) {
        Competition competition = runtime.getCompetition();
        CompetitionSession session = runtime.getCompetitionSession();
        ActiveAssignment activeAssignment = runtime.getActiveAssignment();

        List<AssignmentStatus> assignmentStatuses = session.getAssignmentStatuses();
        List<AssignmentVO> assignments = new ArrayList<>();
        competition.getAssignmentsInOrder().forEach(ca -> {
            Optional<AssignmentStatus> as = assignmentStatuses.stream()
                    .filter(a -> a.getAssignment().equals(ca.getAssignment()))
                    .findFirst();
            assignments.add(toAssignmentVO(ca, as));
        });

        ActiveAssignmentVO active = null;
        if (activeAssignment.isRunning()) {
            Optional<CompetitionAssignment> oca = competition.getAssignments()
                    .stream()
                    .filter(a -> a.getAssignment().equals(activeAssignment.getAssignment()))
                    .findFirst();
            if (oca.isPresent()) {
                Optional<AssignmentStatus> as = assignmentStatuses.stream()
                        .filter(a -> a.getAssignment().equals(activeAssignment.getAssignment()))
                        .findFirst();
                active = ActiveAssignmentVO.builder()
                        .assignment(toAssignmentVO(oca.get(), as))
                        .seconds(oca.get().getAssignment().getAssignmentDuration().toSeconds())
                        .secondsLeft(activeAssignment.getTimeRemaining().toSeconds())
                        .build();
            }
        }

        return CompetitionSessionVO.builder()
                .assignments(assignments)
                .activeAssignment(active)
                .uuid(session.getUuid())
                .name(competition.getName())
                .build();
    }

    private AssignmentVO toAssignmentVO(CompetitionAssignment ca, Optional<AssignmentStatus> as) {
        return AssignmentVO.builder()
                .idx(ca.getOrder())
                .uuid(ca.getAssignment().getUuid())
                .name(ca.getAssignment().getName())
                .collection(ca.getAssignment().getCollection())
                .started(as.map(AssignmentStatus::getDateTimeStart).orElse(null))
                .ended(as.map(AssignmentStatus::getDateTimeEnd).orElse(null))
                .duration(ca.getAssignment().getAssignmentDuration())
                .submits(ca.getAssignment().getAllowedSubmits())
                .build();
    }

    private AssignmentVO toAssignmentVO(Assignment assignment) {
        return AssignmentVO.builder()
                .idx(-1)
                .uuid(assignment.getUuid())
                .name(assignment.getName())
                .collection(assignment.getCollection())
                .duration(assignment.getAssignmentDuration())
                .submits(assignment.getAllowedSubmits())
                .build();
    }

    @Value
    @Builder
    public static class CompetitionVO {
        UUID uuid;
        String name;
        List<AssignmentVO> assignments;
    }

    @Value
    @Builder
    public static class CompetitionSessionVO {
        UUID uuid;
        String name;
        @Builder.Default
        List<AssignmentVO> assignments = new ArrayList<>();
        ActiveAssignmentVO activeAssignment;

        public boolean isAssignmentActive() {
            return activeAssignment != null;
        }
    }

    @Value
    @Builder
    public static class AssignmentVO {
        UUID uuid;
        String name;
        String collection;
        int idx;
        Instant started;
        Instant ended;
        Duration duration;
        int submits;
    }

    @Value
    @Builder
    public static class ActiveAssignmentVO {
        AssignmentVO assignment;
        long secondsLeft;
        long seconds;
    }

    /**
     * General Taskmessage for GUI-actions
     */
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskMessage {
        private String taskName;
        private String uuid;
        private String value;

        public TaskMessage(String taskName) {
            this.taskName = taskName;
        }
    }
}
