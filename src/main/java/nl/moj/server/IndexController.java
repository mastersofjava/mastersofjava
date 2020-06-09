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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.model.CompetitionSession;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@AllArgsConstructor
public class IndexController {

    private CompetitionRuntime competition;
    private TeamRepository teamRepository;
    private TeamService teamService;
    private AssignmentStatusRepository assignmentStatusRepository;
    private AssignmentRepository assignmentRepository;

    @GetMapping("/")
    public String index(Model model, @AuthenticationPrincipal Principal user) {
        if (competition.getCurrentAssignment() == null) {
            model.addAttribute("team", user.getName());
            return "index";
        }
        addModelDataForUserWithAssignment(model, user, competition.getActiveAssignment());
        return "index";
    }

    private boolean isAuthorized(Principal user, HttpServletRequest request) {
        return user!=null &&  user.getName().equalsIgnoreCase("admin") && request.getParameterMap().containsKey("assignment");
    }
    private boolean isUsingCurrentCompetitionAssignment(Assignment assignment) {
        return competition.getCurrentAssignment()!=null && assignment.equals(competition.getActiveAssignment().getAssignment());
    }
    @GetMapping("/assignmentAdmin")
    public String viewAsAdmin(Model model, @AuthenticationPrincipal Principal user, HttpServletRequest request) {
        if (!isAuthorized(user, request)) {
            return "login";
        }
        String solutionInputFileName = request.getParameter("solution");
        boolean isWithAdminValidation = request.getParameterMap().containsKey("validate");
        Assignment assignment = assignmentRepository.findByName(request.getParameter("assignment"));
        Assert.isTrue(assignment!=null,"unauthorized");

        if (isUsingCurrentCompetitionAssignment(assignment)) {
            log.info("solution '" + solutionInputFileName+ "'");
            addModelDataForUserWithAssignment(model, user, competition.getActiveAssignment(), solutionInputFileName, isWithAdminValidation);
        } else {
            addModelDataForAdmin(model, user, assignment, solutionInputFileName, isWithAdminValidation);
        }
        return "index";
    }

    private void addModelDataForAdmin(Model model, Principal user, Assignment assignment, String solutionInputFileName, boolean isWithValidation) {
        Team team = teamRepository.findByName(user.getName());
        CodePageModelWrapper codePage = new CodePageModelWrapper(model, user, solutionInputFileName, isWithValidation);
        codePage.saveFiles(competition.getCompetitionSession(), assignment, team);
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
            as = competition.handleLateSignup(team);
        }

        AssignmentStatusHelper ash = new AssignmentStatusHelper(as, state.getAssignmentDescriptor());

        CodePageModelWrapper codePage = new CodePageModelWrapper(model, user, solutionInputFileName, isWithAdminValidation && team.getRole().equals(Role.ADMIN));
        codePage.saveFiles(competition.getCompetitionSession(), state.getAssignment(), team);
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
        }

        public void saveAdminState( Assignment assignment) {
            List<AssignmentFile> tests = new ArrayList<>();
            for (AssignmentFile inputFile: inputFiles) {
                String name = inputFile.getFile().toFile().getName().toLowerCase();
                boolean isTestCase = name.endsWith(".java") && name.contains("test") && inputFile.isReadOnly();
                if (isTestCase) {
                    tests.add(inputFile);
                }
            }
            model.addAttribute("finished",false);
            model.addAttribute("submitDisabled", true);
            model.addAttribute("submittime", 0);
            model.addAttribute("finalscore", 0);
            model.addAttribute("maxSubmits", 1);
            model.addAttribute("submits", 1);
            model.addAttribute("assignment", assignment.getName());
            model.addAttribute("timeLeft", 0);
            model.addAttribute("time", 0);
            model.addAttribute("tests", tests);
            model.addAttribute("running", true);
            model.addAttribute("solution", isWithInsertSolution);
            model.addAttribute("isWithValidation", isWithValidation);
            model.addAttribute("labels", new ArrayList<>());
            model.addAttribute("bonus", "");
            model.addAttribute("assignmentName", assignment.getName());
        }

        public void saveAssignmentDetails(ActiveAssignment state) {
            List<String> scoreLabels = new ArrayList<>();
            for (String label :state.getAssignmentDescriptor().getLabels()) {
                if (label.startsWith("test")) {
                    scoreLabels.add(label);
                }
            }
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
}
