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
        addModelDataForUserWithAssignment(model, user, competition.getActiveAssignment(), null);
        return "index";
    }
    @GetMapping("/assignmentAdmin")
    public String viewAsAdmin(Model model, @AuthenticationPrincipal Principal user, HttpServletRequest request) {
        boolean isAuthorized = user!=null &&  user.getName().equalsIgnoreCase("admin") && request.getParameterMap().containsKey("assignment");
        if (!isAuthorized) {
            return "login";
        }
        String isWithSolution = request.getParameter("solution");
        Assignment assignment = assignmentRepository.findByName(request.getParameter("assignment"));
        Assert.isTrue(assignment!=null,"unauthorized");

        if (competition.getCurrentAssignment()!=null && assignment.equals(competition.getActiveAssignment().getAssignment())) {
            log.info("solution '" + isWithSolution+ "'");
            addModelDataForUserWithAssignment(model, user, competition.getActiveAssignment(), isWithSolution);
        } else {
            addModelDataForAdmin(model, user, assignment, isWithSolution);
        }
        return "index";
    }
    private void addModelDataForAdmin(Model model, Principal user, Assignment assignment, String isWithSolution) {
        Team team = teamRepository.findByName(user.getName());
        CodePageModelWrapper codePage = new CodePageModelWrapper(model, user, isWithSolution);
        codePage.saveFiles(competition.getCompetitionSession(), assignment, team);
        codePage.saveAdminState(assignment);
    }
    private void addModelDataForUserWithAssignment(Model model, Principal user, ActiveAssignment state, String isWithSolution) {
        Team team = teamRepository.findByName(user.getName());
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(), state
                .getCompetitionSession(), team);

        //late signup
        if (as == null) {
            as = competition.handleLateSignup(team);
        }

        AssignmentStatusHelper ash = new AssignmentStatusHelper(as, state.getAssignmentDescriptor());

        CodePageModelWrapper codePage = new CodePageModelWrapper(model, user, isWithSolution);
        codePage.saveFiles(competition.getCompetitionSession(), state.getAssignment(), team);
        codePage.saveAssignmentDetails(state);
        codePage.saveTeamState(ash);
    }



    private class CodePageModelWrapper {
        private Model model;
        private List<AssignmentFile> inputFiles;
        private String solutionToken;
        private boolean isWithInsertSolution;
        public CodePageModelWrapper(Model model, Principal user, String solutionToken) {
            this.model = model ;
            this.solutionToken = solutionToken;
            isWithInsertSolution = solutionToken != null;
            model.addAttribute("team", user.getName());
        }
        public void saveFiles(CompetitionSession session, Assignment assignment, Team team) {
            List<AssignmentFile> sessionList = teamService.getTeamAssignmentFiles(session, assignment, team);
            inputFiles = new ArrayList<>();
            for (AssignmentFile file: sessionList) {
                if (file.isReadOnly()||!isWithInsertSolution) {
                    inputFiles.add(file);
                } else {
                    JavaAssignmentFileResolver resolver = new JavaAssignmentFileResolver();
                    String path = file.getAbsoluteFile().toFile().getPath().replace("src\\main\\java","assets").replace("src/main/java","assets");
                    File solutionFile = new File(path.replace(".java",solutionToken+"Solution.java"));
                    if (!solutionToken.isEmpty()) {
                        log.info("solutionFile (token=" +solutionToken+ ")=" + solutionFile+ ", exist: " + solutionFile.exists() );
                    }
                    if (solutionFile.exists()) {
                        file = resolver.convertToAssignmentFile(file.getName(), solutionFile.toPath(), file.getBase(), solutionFile.toPath(), AssignmentFileType.EDIT, false, file.getUuid());
                    }

                    inputFiles.add(file);
                }

            }
            // TODO ugly
            inputFiles.sort((arg0, arg1) -> {
                if (arg0.getFileType().equals(AssignmentFileType.TASK)) {
                    return -10;
                }
                return 10;
            });
            model.addAttribute("files", inputFiles);
        }
        public void saveTeamState(AssignmentStatusHelper ash) {
            model.addAttribute("finished", ash.isCompleted());
            model.addAttribute("submittime", ash.getSubmitTime());
            model.addAttribute("finalscore", ash.getScore());
            model.addAttribute("maxSubmits", ash.getMaximumSubmits());
            model.addAttribute("submits", ash.getRemainingSubmits());
            model.addAttribute("solution", isWithInsertSolution);
        }
        public void saveAdminState( Assignment assignment) {
            model.addAttribute("finished",false);
            model.addAttribute("submittime", 0);
            model.addAttribute("finalscore", 0);
            model.addAttribute("maxSubmits", 1);
            model.addAttribute("submits", 1);
            model.addAttribute("assignment", assignment.getName());
            model.addAttribute("timeLeft", 0);
            model.addAttribute("time", 0);
            model.addAttribute("tests", inputFiles);
            model.addAttribute("running", true);
            model.addAttribute("solution", isWithInsertSolution);
            model.addAttribute("labels", new ArrayList<>());
            model.addAttribute("bonus", "");

        }
        public void saveAssignmentDetails(ActiveAssignment state) {
            List<String> scoreLabels = new ArrayList<>();
            for (String label :state.getAssignmentDescriptor().getLabels()) {
                if (label.startsWith("test")) {
                    scoreLabels.add(label);
                }
            }
            model.addAttribute("assignment", state.getAssignmentDescriptor().getDisplayName());
            model.addAttribute("labels", scoreLabels);
            model.addAttribute("bonus", "Success bonus: " +state.getAssignmentDescriptor().getScoringRules().getSuccessBonus());
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
