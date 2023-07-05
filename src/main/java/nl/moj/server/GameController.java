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
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.*;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.controller.TeamForm;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.transaction.Transactional;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Controller
@AllArgsConstructor
@Slf4j
public class GameController {

    private CompetitionRuntime competition;
    private TeamService teamService;
    private UserService userService;
    private TeamAssignmentStatusRepository teamAssignmentStatusRepository;

    @Transactional
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("year", LocalDate.now().getYear());
        return "index";
    }

    
    @GetMapping("/play")
    @Transactional
    public String play(Model model, Principal principal) {

        User user = userService.createOrUpdate(principal);
        Team team = user.getTeam();

        // if there is no team for this user, create one, we found a new
        // signup from the IDP (IDentity Provider).
        if (team == null) {
            // send to team create screen!
            log.info("No team for user {}, redirecting to team creation.", user.getName());
            model.addAttribute("teamForm", new TeamForm());
            return "createteam";
        }

        ActiveAssignment activeAssignment = competition.getActiveAssignment(team);
        model.addAttribute("team", team.getName());
        model.addAttribute("sessionId", competition.getSessionId());
		model.addAttribute("assignmentActive", activeAssignment.isRunning());

        if (activeAssignment.isRunning()) {
            TeamAssignmentStatus as = teamAssignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(activeAssignment.getAssignment(), activeAssignment
                    .getCompetitionSession(), team).orElse(null);
            if (as == null) {
                as = competition.handleLateSignup(team);
            }
            model.addAttribute("assignmentId", activeAssignment.getAssignment().getUuid());
            model.addAttribute("teamStarted", as.isStarted());
            addTeamAssignmentStateToModel(model, activeAssignment, as);
        }
        return "play";
    }


    private void addTeamAssignmentStateToModel(Model model, ActiveAssignment state, TeamAssignmentStatus as) {
        AssignmentDescriptor ad = state.getAssignmentDescriptor();
        List<AssignmentFile> files = teamService.getTeamAssignmentFiles(as.getTeam().getUuid(), competition.getSessionId(), state.getAssignment().getUuid());

        // TODO ugly
        files.sort((arg0, arg1) -> {
            if (arg0.getFileType().equals(AssignmentFileType.TASK)) {
                return -10;
            }
            return 10;
        });

        boolean completed = as.getDateTimeCompleted() != null;
//        || as.getRemainingSubmitAttempts() <= 0
//                || as.getSubmitAttempts().stream().anyMatch(sa -> sa.getSuccess() != null && sa.getSuccess());

        AssignmentResult ar = as.getAssignmentResult();

        TeamAssignmentModel tam = TeamAssignmentModel.builder()
                .assignmentName(state.getAssignmentDescriptor().getDisplayName())
                .teamName(as.getTeam().getName())
                .timeRemaining(state.getTimeRemaining())
                .time(state.getAssignmentDescriptor().getDuration())
                .tests(state.getTestFiles())
                .files(files)
                .completed(completed)
                .score(ar != null ? ar.getFinalScore() : 0L)
                .submits(ad.getScoringRules().getMaximumResubmits() + 1)
                .submitsRemaining(as.getRemainingSubmitAttempts())
                // TODO this is used to render the clock after submit, can we do this smarter?
                .submitTime(ar != null ? ar.getInitialScore() : 0L)
                .build();

        model.addAttribute("assignment", tam);
    }

    @Getter
    @Builder
    public static class TeamAssignmentModel {
        private final String assignmentName;
        private final String teamName;
        private final Duration timeRemaining;
        private final Duration time;
        private final List<AssignmentFile> tests;
        private final List<AssignmentFile> files;
        private final boolean completed;
        private final Long submitTime;
        private final Long score;
        private final int submits;
        private final int submitsRemaining;
    }
}
