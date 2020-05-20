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
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.AssignmentRuntime;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
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

    @ModelAttribute(name = "sessions")
    public List<CompetitionSession> sessions() {
        return competition.getSessions();
    }

    @ModelAttribute(name = "assignments")
    public List<AssignmentDescriptor> assignments() {
        return competition.getAssignmentInfo();
    }
    @ModelAttribute(name = "locationList")
    public List<File> locationList() {
        List<File> locationList = new ArrayList<>();
        File defaultLocation = mojServerProperties.getAssignmentRepo().toFile();
        if (!defaultLocation.exists()) {
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
        for (File file: defaultLocation.getParentFile().listFiles()) {
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
    public String clearAssignments() {
        log.warn("clearAssignments entered");
        competition.getCompetitionState().getCompletedAssignments().clear();

        if (assignmentStatusRepository.count()==0) {
            return "competition not started yet";
        }
        assignmentStatusRepository.deleteAll();// correct cleaning: first delete all status items, afterwards delete all results
        assignmentResultRepository.deleteAll();
        return "competition restarted, reloading page";
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
    @MessageMapping("/control/scanAssignments")
    @SendToUser("/queue/controlfeedback")
    public String cloneAssignmentsRepo(TaskMessage message) {
        try {
            Path path = mojServerProperties.getAssignmentRepo();
            if (StringUtils.isNumeric(message.taskName)) {
                path = getLocationByYear(Integer.parseInt(message.taskName)).toPath();
            }
            log.info("year " + message.taskName + " path " + path + " " +StringUtils.isNumeric(message.taskName)) ;
            List<Assignment> assignmentList = assignmentService.updateAssignments(path);

            log.info("assignmentList " +assignmentList.size() + " " + assignmentList) ;

            Competition c = competition.getCompetition();
            c.setName(path.toFile().getName());
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

            return "Assignments scanned from location "+path+" ("+assignmentList.size()+"), reloading to show them.";
        } catch (AssignmentServiceException ase) {
            log.error("Scanning assignments failed.", ase);
            return ase.getMessage();
        }
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
    public String taskControl(Model model, @ModelAttribute("selectSessionForm") SelectSessionForm ssf,
                              @ModelAttribute("newPasswordRequest") NewPasswordRequest npr) {

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
        boolean isWithAssignmentsLoaded = !competition.getAssignmentInfo().isEmpty();

        if (isWithAssignmentsLoaded) {
            model.addAttribute("assignmentDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTable(assignments()) );
            model.addAttribute("gameDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTableForAssignmentStatus());
        } else {
            model.addAttribute("assignmentDetailCanvas", "(U moet eerst de opdrachten inladen)");
            model.addAttribute("gameDetailCanvas", "(U moet eerst de opdrachten inladen)");
        }

        List<Team> teams = team();
        if (!teams.isEmpty()) {
            model.addAttribute("teamDetailCanvas", gamemasterTableComponents.toSimpleBootstrapTableForTeams(teams));
        } else {
            model.addAttribute("teamDetailCanvas", "(U moet eerst de gebruikers aanmaken)");
        }

        model.addAttribute("repositoryLocation", getSelectedLocation());
        model.addAttribute("selectedYearLabel", getSelectedYearLabel());

        model.addAttribute("isWithAssignmentsLoaded", isWithAssignmentsLoaded);
        ssf.setSession(competition.getCompetitionSession().getUuid());
        return "control";
    }
    private String getSelectedYearLabel() {
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
        if (c.getName().startsWith("20") && new File(file.getParentFile(),c.getName()).isDirectory()) {
            file = new File(file.getParentFile(),c.getName());
        }
        return file;
    }

    @PostMapping("/control/select-session")
    public String selectSession(@ModelAttribute("sessionSelectForm") SelectSessionForm ssf) {
        competition.loadSession(competition.getCompetition(), ssf.getSession());
        return "redirect:/control";
    }

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

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskMessage {
        private String taskName;
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
