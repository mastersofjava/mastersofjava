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
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.authorization.Role;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.controller.TeamForm;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import nl.moj.server.util.HttpUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@AllArgsConstructor
public class GameController {

    private final CompetitionRuntime competitionRuntime;
    private final TeamService teamService;
    private final AssignmentStatusRepository assignmentStatusRepository;
    private final CompetitionSessionRepository competitionSessionRepository;
    private final UserService userService;


    public UUID getSelectedSessionId() {
        UUID globalUUID = competitionRuntime.getCompetitionSession().getUuid();
        return HttpUtil.getSelectedUserSession(globalUUID);
    }

    public CompetitionSession getSelectedCompetitionSession() {
        UUID globalUUID = competitionRuntime.getCompetitionSession().getUuid();
        UUID httpUUID = getSelectedSessionId();
        if (globalUUID.equals(httpUUID)) {
            return competitionRuntime.getCompetitionSession();
        }
        return competitionSessionRepository.findByUuid(httpUUID);
    }

    public CompetitionRuntime getCompetitionRuntimeForGameStart() {
        Assert.isTrue(competitionRuntime != null, "runtime not ready");
        Assert.isTrue(competitionRuntime.getCompetitionSession() != null, "runtime.session not ready");
        UUID globalUUID = competitionRuntime.getCompetitionSession().getUuid();
        UUID httpUUID = HttpUtil.getSelectedUserSession(globalUUID);
        log.debug("httpUUID " + httpUUID + " globalUUID " + globalUUID);
        CompetitionRuntime result = competitionRuntime;
        if (!competitionRuntime.getCompetitionSession().getUuid().equals(httpUUID)) {
            CompetitionSession temp = competitionSessionRepository.findByUuid(httpUUID);
            if (competitionRuntime.isActiveCompetition(temp.getCompetition())) {
                result = competitionRuntime.selectCompetitionRuntimeForGameStart(temp.getCompetition());
            }
        }
        log.debug("result " + result.getCompetitionSession().getUuid());
        return result;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("year", LocalDate.now().getYear());
        return "index";
    }

    @GetMapping("/play")
    public String play(Model model, Principal principal, @ModelAttribute("selectSessionForm") TaskControlController.SelectSessionForm ssf) {

        User user = userService.createOrUpdate(principal);
        Team team = user.getTeam();
        // if there is no team for this user, create on, we found a new
        // signup from the IDP (IDentity Provider).
        if (team == null) {
            // send to team create screen!
            log.info("No team for user {}, redirecting to team creation.", user.getUuid());
            model.addAttribute("teamForm", new TeamForm());
            return "createteam";
        }

        CompetitionSession competitionSession = getSelectedCompetitionSession();
        insertCompetitionSelector(model, ssf, competitionSession.getUuid());
        model.addAttribute("isControlRole", isAdminUser(principal));

        boolean isAvailableAssignment = competitionRuntime.isActiveCompetition(competitionSession.getCompetition());
        if (isAvailableAssignment) {
            CompetitionRuntime runtime = getCompetitionRuntimeForGameStart();
            isAvailableAssignment = runtime.getCompetitionModel().getAssignmentExecutionModel().getOrderedAssignment() != null;
        }
        if (!isAvailableAssignment) {
            model.addAttribute("team", team.getName());
            return "play";
        }
        addModelDataForUserWithAssignment(model, team, competitionRuntime.getActiveAssignment());
        return "play";
    }

    private void insertCompetitionSelector(Model model, TaskControlController.SelectSessionForm ssf, UUID sessionUUID) {
        List<CompetitionSession> sessions = competitionSessionRepository.findAll();
        List<CompetitionSession> activeSessions = new ArrayList<>();
        for (CompetitionSession session : sessions) {
            if (session.isAvailable()) {
                activeSessions.add(session);
            }
        }
        model.addAttribute("sessions", activeSessions);
        UUID input = HttpUtil.getSelectedUserSession(sessionUUID);
        log.debug("input " + input + " activeSessions " + activeSessions.size());
        if (ssf != null) {
            ssf.setSession(input);
        }
    }

    private boolean isAdminUser(Principal user) {
        return Role.isWithControleRole((org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken) user);
    }

    @PostMapping("/index/select-session")
    public String selectSession(@ModelAttribute("sessionSelectForm") SelectSessionForm ssf) {
        HttpUtil.setSelectedUserSession(ssf.getSession());
        return "redirect:/play";
    }

    private void addModelDataForUserWithAssignment(Model model, Team team, ActiveAssignment state) {
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(), state
                .getCompetitionSession(), team);

        //late signup
        if (as == null) {
            as = competitionRuntime.handleLateSignup(team);
        }

        AssignmentStatusHelper ash = new AssignmentStatusHelper(as, state.getAssignmentDescriptor());
        CodePageModelWrapper codePage = new CodePageModelWrapper(model,
                team);
        codePage.saveFiles(competitionRuntime.getCompetitionSession(), state.getAssignment(), team);
        codePage.saveAssignmentDetails(state);
        codePage.saveTeamState(ash);
    }

    private class CodePageModelWrapper {
        private Model model;
        private List<AssignmentFile> inputFiles;

        public CodePageModelWrapper(Model model, Team team) {
            this.model = model;
            model.addAttribute("team", team.getName());
        }

        public void saveFiles(CompetitionSession session, Assignment assignment, Team team) {
            Assert.isTrue(team != null, "invalid team");
            List<AssignmentFile> teamAssignmentFiles = teamService.getTeamAssignmentFiles(session, assignment, team.getUuid());
            model.addAttribute("files", prepareInputFilesOnScreen(teamAssignmentFiles));
        }

        private List<AssignmentFile> prepareInputFilesOnScreen(List<AssignmentFile> teamAssignmentFiles) {
            inputFiles = new ArrayList<>(teamAssignmentFiles);
            // order the task file(s) at the beginning.
            inputFiles.sort((arg0, arg1) -> {
                if (arg0.getFileType().equals(AssignmentFileType.TASK)) {
                    return -10;
                }
                return 10;
            });
            return inputFiles;
        }

        public void saveTeamState(AssignmentStatusHelper ash) {
            boolean isCompleted = ash.isCompleted();
            model.addAttribute("finished", isCompleted);
            model.addAttribute("submittime", ash.getSubmitTime());
            model.addAttribute("finalscore", ash.getScore());
            model.addAttribute("maxSubmits", ash.getMaximumSubmits());
            model.addAttribute("submits", ash.getRemainingSubmits());
            model.addAttribute("submitDisabled", isCompleted);

        }


        private void saveAssignmentDetails(ActiveAssignment state) {
            List<String> scoreLabels = state.getAssignmentDescriptor().readScoreLables();

            model.addAttribute("assignmentName", state.getAssignmentDescriptor().getName());
            model.addAttribute("assignment", state.getAssignmentDescriptor().getDisplayName());
            model.addAttribute("labels", scoreLabels);
            model.addAttribute("bonus", "Success bonus: " + state.getAssignmentDescriptor().getScoringRules().getSuccessBonus());
            model.addAttribute("timeLeft", state.getTimeRemaining());
            model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addAttribute("tests", state.getTestFiles());
            model.addAttribute("running", state.isRunning());

        }
    }

    private class AssignmentStatusHelper {

        private AssignmentStatus assignmentStatus;
        private AssignmentDescriptor assignmentDescriptor;

        public AssignmentStatusHelper(AssignmentStatus assignmentStatus, AssignmentDescriptor assignmentDescriptor) {
            this.assignmentStatus = assignmentStatus;
            this.assignmentDescriptor = assignmentDescriptor;
            Assert.isTrue(this.assignmentStatus != null, "assignmentStatus not ready");
            Assert.isTrue(this.assignmentDescriptor != null, "assignmentDescriptor not ready");

        }

        public boolean isCompleted() {
            return assignmentStatus.getSubmitAttempts()
                    .stream()
                    .anyMatch(SubmitAttempt::isSuccess) ||
                    assignmentStatus.getSubmitAttempts().size() >= (assignmentDescriptor.getScoringRules()
                            .getMaximumResubmits() + 1);
        }

        public long getSubmitTime() {
            return assignmentStatus.getAssignmentResult().getInitialScore();
        }

        public int getMaximumSubmits() {
            return assignmentDescriptor.getScoringRules().getMaximumResubmits() + 1;
        }

        public int getRemainingSubmits() {
            return getMaximumSubmits() - assignmentStatus.getSubmitAttempts().size();
        }

        public long getScore() {
            return assignmentStatus.getAssignmentResult().getFinalScore();
        }
    }

    @Data
    public static class SelectSessionForm {
        private UUID session;
    }
}
