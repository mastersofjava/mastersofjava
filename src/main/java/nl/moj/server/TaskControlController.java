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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.assignment.service.AssignmentServiceException;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionCleaningService;
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.rankings.service.RankingsService;
import nl.moj.server.runtime.AssignmentRuntime;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private final PasswordEncoder encoder;

    private final TeamService teamService;

    private final AssignmentStatusRepository assignmentStatusRepository;

    private final AssignmentResultRepository assignmentResultRepository;

    private final AssignmentRuntime assignmentRuntime;

    private final GamemasterTableComponents gamemasterTableComponents;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final CompetitionCleaningService competitionCleaningService;

    private final RankingsService rankingsService;

    @ModelAttribute(name = "sessions")
    public List<CompetitionSession> sessions() {
        return competition.getSessions();
    }

    public List<AssignmentDescriptor> assignments() {
        List<AssignmentDescriptor> notSortedList = competition.getAssignmentInfo();
        List<String> orderedList = competition.getCompetition().getAssignmentsInOrder().stream().map(OrderedAssignment::getAssignment).map(Assignment::getName).collect(Collectors.toList());

        List<AssignmentDescriptor> officallyOrderedList = new ArrayList<>();
        for (String name : orderedList ) {
            for (AssignmentDescriptor assignmentDescriptor: notSortedList) {
                if (assignmentDescriptor.getName().equals(name)) {
                    officallyOrderedList.add(assignmentDescriptor);
                }
            }
        }
        return officallyOrderedList;
    }
    @ModelAttribute(name = "locationList")
    public List<File> locationList() {
        List<File> locationList = new ArrayList<>();
        File defaultLocation = mojServerProperties.getAssignmentRepo().toFile();
        if (!defaultLocation.exists()||!defaultLocation.getParentFile().isDirectory()) {
            return locationList;
        }
        for (File file: defaultLocation.getParentFile().listFiles()) {
            if (file.isDirectory() && file.getName().startsWith("20")) {
                locationList.add(file);
            }
        }
        return locationList;
    }
    private File getLocationByYear(int year) {
        File defaultLocation = mojServerProperties.getAssignmentRepo().toFile();
        if (year<2008 || year>2030) {
            return defaultLocation;
        }
        String token = ""+year  +"-";
        for (File file: locationList()) {
            if (file.getName().startsWith(token)) {
                return file;
            }
        }
        return defaultLocation;
    }

    @ModelAttribute(name = "teams")
    public List<Team> team() {
        return teamRepository.findAll();
    }

    @MessageMapping("/control/starttask")
    public void startTask(TaskMessage message) {
        competition.startAssignment(message.getTaskName());
    }

    @MessageMapping("/control/stoptask")
    public void stopTask() {
        competition.stopCurrentAssignment();
    }

    @MessageMapping("/control/clearCurrentAssignment")
    @SendToUser("/queue/controlfeedback")
    public String clearCompetition() {
        log.warn("clearCompetition entered");
        gamemasterTableComponents.deleteCurrentSessionResources();
        return competitionCleaningService.doCleanComplete();
    }

    @MessageMapping("/control/pauseResume")
    @SendToUser("/queue/controlfeedback")
    public String pauseResume() {
        log.warn("pauseResume entered");
        ActiveAssignment state = competition.getActiveAssignment();
        if (state==null|| state.getAssignment()==null) {
            return "no active assignment";
        }
        String name = state.getAssignment().getName();
        assignmentRuntime.pauseResume();
        if (assignmentRuntime.isPaused()) {
            return "assignment '"+name+"' paused, reloading page";
        } else {
            return "assignment '"+name+"' running, reloading page";
        }
    }

    public class ConfigurableSettingsService {

        public boolean isRegistrationFormDisabled() {
            return teamRepository.findByName("admin").getCompany().contains("HIDE_REGISTRATION");
        }
        public void setRegistrationFormDisabled(boolean isDisabled) {
            String setting = "HIDE_REGISTRATION";
            if (!isDisabled) {
                setting = "";
            }
            Team team = teamRepository.findByName("admin");//
            team.setCompany(setting);
            teamRepository.save(team);
        }
    }
    private ConfigurableSettingsService configurableSettingsService = new ConfigurableSettingsService();

    @MessageMapping("/control/updateSettingRegistration")
    @SendToUser("/queue/controlfeedback")
    public String updateSettingRegistration(TaskMessage message) {
        boolean isDisable = "true".equals(message.value);
        log.info("update.isDisable " +isDisable + " val " + message.value);
        configurableSettingsService.setRegistrationFormDisabled(isDisable);
        if (isDisable) {
            return "registration form is disabled, users cannot register";
        } else {
            return "registration form is enabled, users can register";
        }
    }
    @MessageMapping("/control/restartAssignment")
    @SendToUser("/queue/controlfeedback")
    public String restartAssignment(TaskMessage message) {
        log.warn("restartAssignment entered: " +message.taskName);
        competition.getCompetitionState().getCompletedAssignments().clear();
        ActiveAssignment state = competition.getActiveAssignment();
        boolean isStopCurrentAssignment=state!=null && state.getAssignment()!=null && state.getAssignment().getName().equals(message.taskName);

        log.warn("isStopCurrentAssignment " + isStopCurrentAssignment);

        if (isStopCurrentAssignment) {
            competition.stopCurrentAssignment();
        }
        Assignment assignment = assignmentRepository.findByName(message.taskName);
        List<AssignmentStatus> ready4deletionList = assignmentStatusRepository.findByAssignmentAndCompetitionSession(assignment, competition.getCompetitionSession());

        log.warn("ready4deletionList " + ready4deletionList.size());
        if (ready4deletionList.isEmpty()) {
            return "Assignment not started yet: "  + message.taskName;
        }
        for (AssignmentStatus status: ready4deletionList) {
            assignmentStatusRepository.deleteById(status.getId());// correct cleaning: first delete all status items, afterwards delete all results
        }
        return "Assignment resetted: " + message.taskName + ", reload page";
    }


    @MessageMapping("/control/competitionSaveName")
    public void doCompetitionSaveName(TaskControlController.TaskMessage message)  throws JsonProcessingException {
        log.info("doCompetitionSaveName " + message + " " + message.getValue() );

        UUID uuid = UUID.fromString(message.getUuid());
        Competition competition =  competitionRepository.findByUuid(uuid);
        String[] parts = competition.getName().split("\\|");
        String input = message.getValue();
        if (parts.length>=2) {
            input = message.getValue() + "|" +parts[1];
        }
        competition.setName(input );
        competitionRepository.save(competition);

    }
    @MessageMapping("/control/competitionDelete")
    @SendToUser("/queue/controlfeedback")
   // @Transactional
    public String deleteCompetition(TaskControlController.TaskMessage message) {
        boolean isUpdateCurrentCompetition =  message.getUuid().equals(competition.getCompetition().getUuid());
        log.info("deleteCompetition " +message.getUuid() + " isUpdateCurrentCompetition " +isUpdateCurrentCompetition);

        try {
            long startAmount = competitionRepository.count();
            Competition competitionToClean = competitionRepository.findByUuid(UUID.fromString(message.getUuid()));
            List<CompetitionSession> sessionsToDelete = competitionSessionRepository.findByCompetition(competitionToClean);
            log.info("sessionsToDelete " +sessionsToDelete.size());
            if (isUpdateCurrentCompetition) {
                clearCompetition();
            }
            for (CompetitionSession session: sessionsToDelete) {
                competitionSessionRepository.delete(session);
            }
            if (startAmount>1) {

                competitionRepository.delete(competitionToClean);
                if (isUpdateCurrentCompetition) {
                    List<Competition> list = competitionRepository.findAll();

                    competition.loadMostRecentSession(list.get(0));
                }
            } else {
                competition.startSession(competitionToClean); // always guarantee at least one competition.
            }


        } catch (Exception ex) {
            log.info("Error during deletion", ex);
            return "Error during deletion";
        }
        return "Deleted competition, now reloading page";
    }

    @MessageMapping("/control/competitionCreateNew")
    @SendToUser("/queue/controlfeedback")
    public String doCompetitionCreateNew(TaskControlController.TaskMessage message)  throws JsonProcessingException {
        log.info("doCompetitionCreateNew " + message + " " + message.getValue() );
        if (StringUtils.isBlank(message.value)) {
            return "Please provide a valid name. ";
        }
        Competition c = new Competition();
        c.setUuid(UUID.randomUUID());
        c.setName(message.getValue());
        c = competitionRepository.save(c);
        c.setAssignments(assignmentRepository.findAll()
                .stream()
                .map(createOrderedAssignments(c))
                .collect(Collectors.toList()));
        c = competitionRepository.save(c);

        competition.startSession(c);
        return "New competition created, reloading page";
    }
    @MessageMapping("/control/updateTeamStatus")
    @SendToUser("/queue/controlfeedback")
    public String doUpdateUserStatus(TaskControlController.TaskMessage message)  throws JsonProcessingException {
        log.info("updateUserStatus " + message + " " + message.getValue() );
        if (StringUtils.isBlank(message.uuid)) {
            return "Please provide valid input. ";
        }
        Team team = teamRepository.findByUuid(UUID.fromString(message.getUuid()));
        if (team==null) {
            return "Team already deleted.";
        }
        if (message.value.equals("DISQUALIFY")) {
            team.setCompany(message.value);
            team.setRole(Role.ANONYMOUS);
            teamRepository.save(team);
            return "Disqualify team '"+team.getName()+"'";
        } else
        if (message.value.equals("ARCHIVE")) {
            team.setCompany(message.value);
            team.setRole(Role.ANONYMOUS);
            teamRepository.delete(team);
            return "Deleted team '"+team.getName()+"'";
        } else {
            team.setCompany(message.value);
            teamRepository.save(team);
        }
        return "Updated team '"+team.getName()+"'";
    }

    /**
     * import assignments from a selected available year.
     * @param message
     * @return
     */
    @MessageMapping("/control/scanAssignments")
    @SendToUser("/queue/controlfeedback")
    public String cloneAssignmentsRepo(TaskMessage message) {
        try {
            Path path = mojServerProperties.getAssignmentRepo();
            if (StringUtils.isNumeric(message.taskName)) {
                path = getLocationByYear(Integer.parseInt(message.taskName)).toPath();
            }
            if (!path.toFile().isDirectory()) {
                return "Assignment location invalid ("+path+").";
            }
            log.info("year " + message.taskName + " path " + path + " " +StringUtils.isNumeric(message.taskName)) ;
            List<Assignment> assignmentList = assignmentService.updateAssignments(path);
            if (assignmentList.isEmpty()) {
                return "No assignments scanned from location of "+path.toFile().getName()+" (improve assignments before importing).";
            }
            log.info("assignmentList " +assignmentList.size() + " " + assignmentList) ;

            Competition c = competition.getCompetition();

            String name = c.getName().split("\\|")[0]+ "|" + path.toFile().getName();

            startCompetitionWithFreshAssignments(name);

            return "Assignments scanned from location "+path+" ("+assignmentList.size()+"), reloading to show them.";
        } catch (AssignmentServiceException ase) {
            log.error("Scanning assignments failed.", ase);
            return ase.getMessage();
        }
    }
    private void startCompetitionWithFreshAssignments(String name) {
        Competition c = competition.getCompetition();

        c.setName(name);
        // wipe previous assignments
        c.setAssignments(new ArrayList<>());
        c = competitionRepository.save(c);

        Assert.isTrue( c.getAssignments().isEmpty(),"competition should have no assignments");
        // re-add updated assignments
        c.setAssignments(assignmentRepository.findAll()
                .stream()
                .map(createOrderedAssignments(c))
                .collect(Collectors.toList()));
        c = competitionRepository.save(c);
        log.info("assignment repository " +c.getAssignments().size() + " " + c.getAssignmentsInOrder().size()) ;

        competition.loadSession(c, competition.getCompetitionSession().getUuid());
    }

    private Function<Assignment, OrderedAssignment> createOrderedAssignments(Competition c) {
        AtomicInteger count = new AtomicInteger(0);
        return a -> {
            OrderedAssignment oa = new OrderedAssignment();
            oa.setAssignment(a);
            oa.setCompetition(c);
            oa.setUuid(UUID.randomUUID());
            oa.setOrder(count.getAndIncrement());
            return oa;
        };
    }

    @GetMapping("/control")
    public String taskControl(Model model, @AuthenticationPrincipal Authentication user, @ModelAttribute("selectSessionForm") SelectSessionForm ssf,
                              @ModelAttribute("newPasswordRequest") NewPasswordRequest npr) {
        if (sessions().isEmpty()) {
            competition.startSession(competition.getCompetition());
        }
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        String selectedYearLabel = getSelectedYearLabel();
        boolean isWithAdminRole = roles.contains(Role.ADMIN);
        boolean isWithSecretCurrentYear = selectedYearLabel.contains("2020");

        Assert.isTrue(roles.contains(Role.ADMIN)||roles.contains(Role.GAME_MASTER),"not authorized");
        Assert.isTrue(!isWithSecretCurrentYear||isWithAdminRole,"Gamemasters are not authorized to see secret current year assignments");

        if (competition.getCurrentAssignment() != null) {
            ActiveAssignment state = competition.getActiveAssignment();
            model.addAttribute("timeLeft", state.getTimeRemaining());
            model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addAttribute("running", state.isRunning());
            model.addAttribute("clockStyle", (assignmentRuntime.isPaused()?"disabled":"active"));
            model.addAttribute("currentAssignment", state.getAssignmentDescriptor().getName());
        } else {
            model.addAttribute("timeLeft", 0);
            model.addAttribute("time", 0);
            model.addAttribute("running", false);
            model.addAttribute("clockStyle", "active");
            model.addAttribute("currentAssignment", "-");
        }
        model.addAttribute("isWithAdminRole", isWithAdminRole);
        model.addAttribute("setting_registration_disabled", configurableSettingsService.isRegistrationFormDisabled());
        List<AssignmentDescriptor> assignmentDescriptorList = assignments();
        boolean isWithAssignmentsLoaded = !assignmentDescriptorList.isEmpty();

        if (isWithAssignmentsLoaded) {
            String assignmentDetailCanvas = gamemasterTableComponents.toSimpleBootstrapTable(assignmentDescriptorList);

            model.addAttribute("isWithConfigurableTestScore",assignmentDetailCanvas.contains("(*2)"));
            model.addAttribute("isWithHiddenTests",assignmentDetailCanvas.contains("(*1)"));
            model.addAttribute("assignmentDetailCanvas",  assignmentDetailCanvas);
            model.addAttribute("gameDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTableForAssignmentStatus());
            model.addAttribute("opdrachtConfiguraties", gamemasterTableComponents.toSimpleBootstrapTablesForFileDetails(assignmentDescriptorList));
        } else {
            model.addAttribute("assignmentDetailCanvas", "(U moet eerst de opdrachten inladen)");
            model.addAttribute("gameDetailCanvas", "(U moet eerst de opdrachten inladen)");
            model.addAttribute("opdrachtConfiguraties","(U moet eerst de opdrachten inladen)");
            model.addAttribute("isWithConfigurableTestScore",false);
            model.addAttribute("isWithHiddenTests",false);
        }
        model.addAttribute("assignments",assignmentDescriptorList);

        List<Team> teams = team();
        if (!teams.isEmpty() && isWithAdminRole) {
            List<Ranking> rankings = rankingsService.getRankings(competition.getCompetitionSession());
            model.addAttribute("teamDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTableForTeams(teams, true, rankings));
            model.addAttribute("activeTeamDetailCanvas",  gamemasterTableComponents.toSimpleBootstrapTableForTeams(teams, false, rankings));
        } else {
            model.addAttribute("activeTeamDetailCanvas", "(U moet eerst de gebruikers aanmaken)");
            model.addAttribute("teamDetailCanvas", "(U moet eerst de gebruikers aanmaken)");
        }
        if (isWithAdminRole) {
            model.addAttribute("repositoryLocation", getSelectedLocation());
            model.addAttribute("selectedYearLabel", selectedYearLabel);
        } else {
            model.addAttribute("repositoryLocation", mojServerProperties.getAssignmentRepo().toFile());
            model.addAttribute("selectedYearLabel", "");
        }

        model.addAttribute("sessionDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTableForSessions());
        model.addAttribute("isWithAssignmentsLoaded", isWithAssignmentsLoaded);
        ssf.setSession(competition.getCompetitionSession().getUuid());
        return "control";
    }
    private String getSelectedYearLabel() {
        if (competition.getAssignmentInfo().isEmpty()) {
            return "";
        }
        String year = getSelectedLocation().getName().split("-")[0];
        if (!StringUtils.isNumeric(year)) {
            year = "";
        } else {
            year = " ("+year + ")";
        }
        return year;
    }
    private File getSelectedLocation() {
        File file = mojServerProperties.getAssignmentRepo().toFile();
        Competition c = competition.getCompetition();
        if (!c.getName().contains("|20")) {
            return file;
        }
        String name = c.getName().split("\\|")[1];
        if (new File(file.getParentFile(),name).isDirectory()) {
            file = new File(file.getParentFile(),name);
        }
        return file;
    }

    @PostMapping("/control/select-session")
    public String selectSession(@ModelAttribute("sessionSelectForm") SelectSessionForm ssf) {
        competition.loadSession(competition.getCompetition(), ssf.getSession());
        return "redirect:/control";
    }

    /**
     * creates new session in selected competition. There is always only on session active in a competition.
     */
    @PostMapping("/control/new-session")
    public String newSession() {
        competition.startSession(competition.getCompetition());

        return "redirect:/control";
    }





    @PostMapping("/control/resetPassword")
    public String resetPassword(RedirectAttributes redirectAttributes,
                                @ModelAttribute("newPasswordRequest") NewPasswordRequest passwordChangeRequest) {

        String errorMessage = null;

        if (passwordChangeRequest.teamUuid.equals("0")) {
            errorMessage = "No team selected";
        } else {
            Team team = teamRepository.findByUuid(UUID.fromString(passwordChangeRequest.teamUuid));
            if (passwordChangeRequest.newPassword == null || passwordChangeRequest.newPassword.isBlank()) {
                errorMessage = "New password can't be empty";
            } else if (!passwordChangeRequest.newPassword.equals(passwordChangeRequest.newPasswordCheck)) {
                errorMessage = "Password and confirmaton did not match";
            } else {
                team.setPassword(encoder.encode(passwordChangeRequest.newPassword));
                teamRepository.save(team);
                redirectAttributes.addFlashAttribute("success", "Successfully changed password");
                return "redirect:/control";
            }
        }

        passwordChangeRequest.clearPasswords();
        redirectAttributes.addFlashAttribute("newPasswordRequest", passwordChangeRequest);
        redirectAttributes.addFlashAttribute("error", errorMessage);

        return "redirect:/control";
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

    @Data
    public static class NewPasswordRequest {
        private String teamUuid;
        private String newPassword;
        private String newPasswordCheck;

        public void clearPasswords() {
            newPassword = null;
            newPasswordCheck = null;
        }
    }

    @Data
    public static class SelectSessionForm {
        private UUID session;
    }



}
