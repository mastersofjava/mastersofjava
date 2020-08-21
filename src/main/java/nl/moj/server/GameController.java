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

import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.login.SignupForm;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.JavaAssignmentFileResolver;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.HttpUtil;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@AllArgsConstructor
public class GameController {

    private CompetitionRuntime competitionRuntime;
    private TeamRepository teamRepository;
    private TeamService teamService;
    private AssignmentStatusRepository assignmentStatusRepository;
    private AssignmentRepository assignmentRepository;
	private CompetitionService competitionService;
    private CompetitionSessionRepository competitionSessionRepository;


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
        Assert.isTrue(competitionRuntime!=null,"runtime not ready");
        Assert.isTrue(competitionRuntime.getCompetitionSession()!=null,"runtime.session not ready");
        UUID globalUUID = competitionRuntime.getCompetitionSession().getUuid();
        UUID httpUUID = HttpUtil.getSelectedUserSession(globalUUID);
        log.debug("httpUUID " + httpUUID+ " globalUUID " + globalUUID);
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

    @GetMapping("/play")
    public String index(Model model, @AuthenticationPrincipal Principal user,@ModelAttribute("selectSessionForm") TaskControlController.SelectSessionForm ssf) {
        log.info("user " +user);
        if (user==null) {
            model.addAttribute("isControlRole", isAdminUser(user));
            return "play";
        }
		// The admin user should be created with the bootstrap
        boolean isWithExistingKcUser = doesUserExist(user);
        log.info("isWithExistingKcUser " +isWithExistingKcUser);
		if ((!isWithExistingKcUser && !isAdminUser(user))) {
            log.info("user " +user.getName());
			createNewTeam(user);
        }
        CompetitionSession competitionSession = getSelectedCompetitionSession();
        insertCompetitionSelector(model, ssf, competitionSession.getUuid());
        model.addAttribute("isControlRole", isAdminUser(user));

        boolean isAvailableAssignment = competitionRuntime.isActiveCompetition(competitionSession.getCompetition());
        if (isAvailableAssignment) {
            CompetitionRuntime runtime = getCompetitionRuntimeForGameStart();
            isAvailableAssignment = runtime.getCompetitionModel().getAssignmentExecutionModel().getOrderedAssignment()!=null;
        }
        if (!isAvailableAssignment ) {
            model.addAttribute("team", user.getName());
            return "play";
        }
        addModelDataForUserWithAssignment(model, user, competitionRuntime.getActiveAssignment());
        return "play";
    }

	private void createNewTeam(Principal user) {
		SignupForm form = SignupForm.builder().company("None").country("NL").name(user.getName()).build();
		competitionService.createNewTeam(form, Role.USER);
	}

	private boolean doesUserExist(Principal user) {
        return teamRepository.findByName(user.getName()) != null;
    }
    private void insertCompetitionSelector(Model model, SelectSessionForm ssf,UUID sessionUUID) {
        List<CompetitionSession> sessions = competitionSessionRepository.findAll();
        List<CompetitionSession> activeSessions = new ArrayList<>();
        for (CompetitionSession session: sessions) {
            if (session.isAvailable()) {
                activeSessions.add(session);
            }
        }
        model.addAttribute("sessions", activeSessions);
        UUID input = HttpUtil.getSelectedUserSession( sessionUUID);
        log.debug("input " + input + " activeSessions " +activeSessions.size());
        if (ssf!=null) ssf.setSession(input);
    }

    private void insertCompetitionSelector(Model model, TaskControlController.SelectSessionForm ssf,UUID sessionUUID) {
        List<CompetitionSession> sessions = competitionSessionRepository.findAll();
        List<CompetitionSession> activeSessions = new ArrayList<>();
        for (CompetitionSession session: sessions) {
            if (session.isAvailable()) {
                activeSessions.add(session);
            }
        }
        model.addAttribute("sessions", activeSessions);
        UUID input = HttpUtil.getSelectedUserSession( sessionUUID);
        log.debug("input " + input + " activeSessions " +activeSessions.size());
        if (ssf!=null) {
            ssf.setSession(input);
        }
    }
    private boolean isAdminUser(Principal user) {
        return Role.isWithControleRole((org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken) user);
    }

    private boolean isAdminAuthorized(Principal user, HttpServletRequest request) {
        return user != null && isAdminUser(user) && request.getParameterMap().containsKey("assignment");
    }

    private boolean isUsingCurrentCompetitionAssignment(Assignment assignment,CompetitionRuntime runtime) {
        return runtime.getCurrentRunningAssignment()!=null && assignment.equals(runtime.getActiveAssignment().getAssignment());
    }

    @GetMapping("/assignmentAdmin")
    public String viewAsAdmin(Model model, @AuthenticationPrincipal Principal user,
                              HttpServletRequest request,@RequestParam("assignment") String assignmentInput,
                              @RequestParam(required = false, name = "solution") String solutionInputFileName,@ModelAttribute("selectSessionForm") TaskControlController.SelectSessionForm ssf) {
        if (!isAdminAuthorized(user, request)) {
            return "redirect:" + KeycloakAuthenticationEntryPoint.DEFAULT_LOGIN_URI;
        }

        CompetitionRuntime runtime = getCompetitionRuntimeForGameStart();

        boolean isWithAdminValidation = request.getParameterMap().containsKey("validate");
        Assignment assignment = assignmentRepository.findByName(assignmentInput);
        Assert.isTrue(assignment != null, "unauthorized");
        log.info("viewAsAdmin.solution {}, assignment {}", solutionInputFileName, assignmentInput);
        if (isUsingCurrentCompetitionAssignment(assignment, runtime)) {

            addModelDataForUserWithAssignment(model, user, runtime.getActiveAssignment(), solutionInputFileName, isWithAdminValidation);
        } else {
            addModelDataForAdmin(model, user, assignment, solutionInputFileName, isWithAdminValidation);
        }
        insertCompetitionSelector(model, ssf, runtime.getCompetitionSession().getUuid());
        model.addAttribute("isControlRole", isAdminUser(user));
        return "play";
    }
    @PostMapping("/index/select-session")
    public String selectSession(@ModelAttribute("sessionSelectForm") SelectSessionForm ssf) {
        HttpUtil.setSelectedUserSession(ssf.getSession());
        return "redirect:/";
    }
    private void addModelDataForAdmin(Model model, Principal user, Assignment assignment, String solutionInputFileName, boolean isWithValidation) {
        Team team = teamRepository.findByName(user.getName());
        CodePageModelWrapper codePage = new CodePageModelWrapper(model, user, solutionInputFileName, isWithValidation);
        codePage.saveFiles(competitionRuntime.getCompetitionSession(), assignment, team);
        codePage.saveAdminState(assignment);
    }

    private void addModelDataForUserWithAssignment(Model model, Principal user, ActiveAssignment state) {
        addModelDataForUserWithAssignment(model, user, state, null, false);
    }

    private void addModelDataForUserWithAssignment(Model model, Principal user, ActiveAssignment state, String solutionInputFileName, boolean isWithAdminValidation) {
        Team team = teamRepository.findByName(user.getName());
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(), state
                .getCompetitionSession(), team);

        //late signup
        if (as == null) {
            as = competitionRuntime.handleLateSignup(team);
        }

        AssignmentStatusHelper ash = new AssignmentStatusHelper(as, state.getAssignmentDescriptor());

		CodePageModelWrapper codePage = new CodePageModelWrapper(model,
				user,
				solutionInputFileName,
				isWithAdminValidation && Role.isWithControleRole(
						(KeycloakAuthenticationToken) user));
		codePage.saveFiles(competitionRuntime.getCompetitionSession(), state.getAssignment(), team);
        codePage.saveAssignmentDetails(state);
        codePage.saveTeamState(ash);
    }

    private class CodePageModelWrapper {
        private Model model;
        private List<AssignmentFile> inputFiles;
        private String solutionToken;
        private boolean isWithInsertSolution;
        private boolean isWithValidation;
        public CodePageModelWrapper(Model model, Principal user, String solutionToken, boolean isWithValidation) {
            this.model = model ;
            this.solutionToken = solutionToken;
            isWithInsertSolution = solutionToken != null;
            this.isWithValidation = isWithValidation;
            model.addAttribute("team", user.getName());
        }
        private AssignmentFile createSolution(AssignmentFile file){
            JavaAssignmentFileResolver resolver = new JavaAssignmentFileResolver();
            String path = file.getAbsoluteFile().toFile().getPath().replace("src\\main\\java","assets").replace("src/main/java","assets");
            File solutionFile = new File(path.replace(".java",solutionToken+"Solution.java"));
            if (!solutionToken.isEmpty()) {
                log.info("solutionFile (token=" +solutionToken+ ")=" + solutionFile+ ", exist: " + solutionFile.exists() );
            }
            if (solutionFile.exists()) {
                file = resolver.convertToAssignmentFile(file.getName(), solutionFile.toPath(), file.getBase(), solutionFile.toPath(), AssignmentFileType.EDIT, false, file.getUuid());
            }
            return file;
        }
        public void saveFiles(CompetitionSession session, Assignment assignment, Team team) {
            Assert.isTrue(team!=null,"invalid team");
            List<AssignmentFile> teamAssignmentFiles = teamService.getTeamAssignmentFiles(session, assignment, team);
            model.addAttribute("files", prepareInputFilesOnScreen(teamAssignmentFiles));
        }
        private List<AssignmentFile> prepareInputFilesOnScreen(List<AssignmentFile> teamAssignmentFiles) {
            inputFiles = new ArrayList<>();
            for (AssignmentFile file: teamAssignmentFiles) {
                if (file.isReadOnly()||!isWithInsertSolution) {
                    inputFiles.add(file);
                } else {
                    // insert solution for the admin (instead of editable file)
                    inputFiles.add(createSolution(file));
                }
            }
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
            model.addAttribute("solution", isWithInsertSolution);
            model.addAttribute("submitDisabled", isCompleted);
            model.addAttribute("isWithValidation", isWithValidation);

        }

        private List<AssignmentFile> filterUnitTestsFromInputFiles() {
            List<AssignmentFile> tests = new ArrayList<>();
            for (AssignmentFile inputFile: inputFiles) {
                String name = inputFile.getFile().toFile().getName().toLowerCase();
                boolean isTestCase = name.endsWith(".java") && name.contains("test") && inputFile.isReadOnly();
                if (isTestCase) {
                    tests.add(inputFile);
                }
            }
            return tests;
        }

        public void saveAdminState( Assignment assignment) {
            model.addAttribute("finished",false);
            model.addAttribute("submitDisabled", false);
            model.addAttribute("submittime", 0);
            model.addAttribute("finalscore", 0);
            model.addAttribute("maxSubmits", 1);
            model.addAttribute("submits", 1);
            model.addAttribute("assignment", assignment.getName());
            model.addAttribute("timeLeft", 0);
            model.addAttribute("time", 0);
            model.addAttribute("tests", filterUnitTestsFromInputFiles());
            model.addAttribute("running", true);
            model.addAttribute("solution", isWithInsertSolution);
            model.addAttribute("isWithValidation", isWithValidation);
            model.addAttribute("labels", new ArrayList<>());
            model.addAttribute("bonus", "");
            model.addAttribute("assignmentName", assignment.getName());
        }

        public void saveAssignmentDetails(ActiveAssignment state) {
            List<String> scoreLabels = state.getAssignmentDescriptor().readScoreLables();

            model.addAttribute("assignmentName", state.getAssignmentDescriptor().getName());
            model.addAttribute("assignment", state.getAssignmentDescriptor().getDisplayName());
            model.addAttribute("labels", scoreLabels);
            model.addAttribute("bonus", "Success bonus: " +state.getAssignmentDescriptor().getScoringRules().getSuccessBonus());
            model.addAttribute("timeLeft", state.getTimeRemaining());
            model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addAttribute("tests", state.getTestFiles());
            model.addAttribute("running", state.isRunning());
            model.addAttribute("isWithValidation", isWithValidation);

        }
    }
    private class AssignmentStatusHelper {

        private AssignmentStatus assignmentStatus;
        private AssignmentDescriptor assignmentDescriptor;

        public AssignmentStatusHelper(AssignmentStatus assignmentStatus, AssignmentDescriptor assignmentDescriptor) {
            this.assignmentStatus = assignmentStatus;
            this.assignmentDescriptor = assignmentDescriptor;
            Assert.isTrue(this.assignmentStatus!=null,"assignmentStatus not ready");
            Assert.isTrue(this.assignmentDescriptor!=null,"assignmentDescriptor not ready");

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
