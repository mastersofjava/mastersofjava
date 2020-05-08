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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.*;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.assignment.service.AssignmentServiceException;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.feedback.FileSubmission;
import nl.moj.server.feedback.Submission;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.CompetitionState;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.security.RolesAllowed;

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

    @ModelAttribute(name = "sessions")
    public List<CompetitionSession> sessions() {
        return competition.getSessions();
    }

    @ModelAttribute(name = "assignments")
    public List<AssignmentDescriptor> assignments() {
        return competition.getAssignmentInfo();
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
    @Transactional
    public void clearAssignments() {
        log.warn("clearAssignments entered");
        competition.getCompetitionState().getCompletedAssignments().clear();
        assignmentResultRepository.deleteAll();
        assignmentStatusRepository.deleteAll();
    }



    @MessageMapping("/control/scanAssignments")
    @SendToUser("/queue/controlfeedback")
    public String cloneAssignmentsRepo() {
        try {
            assignmentService.updateAssignments(mojServerProperties.getAssignmentRepo());

            Competition c = competition.getCompetition();

            // wipe assignments
            c.setAssignments(new ArrayList<>());
            c = competitionRepository.save(c);

            // re-add updated assignments
            c.setAssignments(assignmentRepository.findAll()
                    .stream()
                    .map(createOrderedAssignments(c))
                    .collect(Collectors.toList()));
            c = competitionRepository.save(c);

            competition.loadSession(c, competition.getCompetitionSession().getUuid());

            return "Assignments scanned, reload to show them.";
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

    @GetMapping("/new_game_master")
    public String gamemasterViaNewUri(Model model, @ModelAttribute("selectSessionForm") SelectSessionForm ssf,
                              @ModelAttribute("newPasswordRequest") NewPasswordRequest npr) {

        return taskControl(model, ssf, npr);
    }
    @GetMapping("/control")
    public String taskControl(Model model, @ModelAttribute("selectSessionForm") SelectSessionForm ssf,
                              @ModelAttribute("newPasswordRequest") NewPasswordRequest npr) {

        if (competition.getCurrentAssignment() != null) {
            ActiveAssignment state = competition.getActiveAssignment();
            model.addAttribute("timeLeft", state.getTimeRemaining());
            model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addAttribute("running", state.isRunning());
            model.addAttribute("currentAssignment", state.getAssignmentDescriptor().getName());
        } else {
            model.addAttribute("timeLeft", 0);
            model.addAttribute("time", 0);
            model.addAttribute("running", false);
            model.addAttribute("currentAssignment", "-");
        }
        boolean isWithAssignmentsLoaded = !competition.getAssignmentInfo().isEmpty();
        if (isWithAssignmentsLoaded) {
            model.addAttribute("assignmentDetailCanvas", toSimpleBootstrapTable(assignments()) );
        } else {
            model.addAttribute("assignmentDetailCanvas", "(U moet eerst de opdrachten inladen)");
        }
        if (isWithAssignmentsLoaded) {
            model.addAttribute("gameDetailCanvas", toSimpleBootstrapTableForAssignmentStatus());
        } else {
            model.addAttribute("gameDetailCanvas", "(U moet eerst de opdrachten inladen)");
        }
        List<Team> teams = team();
        if (!teams.isEmpty()) {
            model.addAttribute("teamDetailCanvas", toSimpleBootstrapTableForTeams(teams));
        } else {
            model.addAttribute("teamDetailCanvas", "(U moet eerst de gebruikers aanmaken)");
        }
        ssf.setSession(competition.getCompetitionSession().getUuid());
        return "control";
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


    @GetMapping(value = "/getGameMasterState", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    public @ResponseBody
    Map<String,Object> getAssignmentSolution() {
        Map<String,Object> response = new TreeMap<>();
        response.put("mojServerProperties", mojServerProperties);
        response.put("assignments", competition.getAssignmentInfo());
        response.put("teams", teamRepository.findAll());


        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        if (activeAssignment!=null && activeAssignment.getAssignment()!=null) {
            response.put("activeAssignment.assignment", activeAssignment.getAssignment());
            response.put("activeAssignment.assignmentDescriptor", activeAssignment.getAssignmentDescriptor());

            List<Team> teams = teamService.getTeams();
            if (!teams.isEmpty()) {
                List<AssignmentFile> files = teamService.getTeamAssignmentFiles(competition.getCompetitionSession(), activeAssignment.getAssignment(), teams.get(0));
                response.put("files", files);
            }
        }

        return response;
    }
    private String toSimpleBootstrapTableForTeams(List<Team> teams) {
        StringBuilder sb = new StringBuilder();
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Nr</th><th>Teamnaam</th><th>Score</th></tr></thead>");

        int counter = 1;
        for (Team team: teams) {
            sb.append("<tr><td>"+counter+"</td><td>"+team.getName()+"</td><td>0</td></tr>");
            counter ++;
        }
        sb.append("</table>");
        return sb.toString();
    }
    private String toSimpleBootstrapTableForAssignmentStatus() {
        StringBuilder sb = new StringBuilder();
        List<OrderedAssignment> completedList = competition.getCompetitionState().getCompletedAssignments();
        List<OrderedAssignment> orderedList = competition.getCompetition().getAssignmentsInOrder();
        if (orderedList.isEmpty()) {
            return "";
        }
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Nr</th><th>Opdracht</th><th>Status</th><th>High score</th></tr></thead>");
        for (OrderedAssignment orderedAssignment: orderedList) {
            boolean isCompleted = false;
            boolean isCurrent = orderedAssignment.equals(competition.getCurrentAssignment());

            for (OrderedAssignment completedAssignment: completedList) {
                if (completedAssignment.getAssignment().getName().equals(orderedAssignment.getAssignment().getName())) {
                    isCompleted = true;
                }
            }
            String status = "";
            if (isCompleted) {
                status += " COMPLETED";
            }
            if (isCurrent) {
                status += " <a href='/'>CURRENT</a>";
            }
            if (status.isEmpty()) {
                status = "-";
            }
            sb.append("<tr><td>"+orderedAssignment.getOrder()+"</td><td>"+orderedAssignment.getAssignment().getName()+"</td><td>"+status+"</td><td>0</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String toSimpleBootstrapTable(List<AssignmentDescriptor> assignmentDescriptorList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<br/><table class='roundGrayBorder table' ><thead><tr><th>Opdracht</th><th>Auteur</th><th>Java versie</th><th>Complexiteit</th></tr></thead>");
        for (AssignmentDescriptor descriptor: assignmentDescriptorList) {


            sb.append("<tr><td>"+descriptor.getName()+"</td><td>"+descriptor.getAuthor().getName().split("\\(")[0]+"</td><td>"+descriptor.getJavaVersion()+"</td><td>"+descriptor.getDifficulty() + "</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }
}
