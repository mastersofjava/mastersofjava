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

import java.security.Principal;
import java.util.List;

import lombok.AllArgsConstructor;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.runtime.CompetitionRuntime;
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
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class IndexController {

    private CompetitionRuntime competition;
    private TeamRepository teamRepository;
    private TeamService teamService;
    private AssignmentStatusRepository assignmentStatusRepository;

    @GetMapping("/")
    public String index(Model model, @AuthenticationPrincipal Principal user) {
        if (competition.getCurrentAssignment() == null) {
            model.addAttribute("team", user.getName());
            return "index";
        }
        addModel(model, user);
        return "index";
    }

    private void addModel(Model model, Principal user) {
        ActiveAssignment state = competition.getActiveAssignment();
        Team team = teamRepository.findByName(user.getName());
        AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(), state
                .getCompetitionSession(), team);

        //late signup
        if (as == null) {
            as = competition.handleLateSignup(team);
        }

        AssignmentStatusHelper ash = new AssignmentStatusHelper(as, state.getAssignmentDescriptor());
        List<AssignmentFile> files = teamService.getTeamAssignmentFiles(competition.getCompetitionSession(), state.getAssignment(), team);

        // TODO ugly
        files.sort((arg0, arg1) -> {
            if (arg0.getFileType().equals(AssignmentFileType.TASK)) {
                return -10;
            }
            return 10;
        });

        model.addAttribute("assignment", state.getAssignmentDescriptor().getDisplayName());
        model.addAttribute("team", user.getName());
        model.addAttribute("timeLeft", state.getTimeRemaining());
        model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
        model.addAttribute("tests", state.getTestFiles());
        model.addAttribute("files", files);
        model.addAttribute("running", state.isRunning());
        model.addAttribute("finished", ash.isCompleted());
        model.addAttribute("submittime", ash.getSubmitTime());
        model.addAttribute("finalscore", ash.getScore());
        model.addAttribute("maxSubmits", ash.getMaximumSubmits());
        model.addAttribute("submits", ash.getRemainingSubmits());
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
