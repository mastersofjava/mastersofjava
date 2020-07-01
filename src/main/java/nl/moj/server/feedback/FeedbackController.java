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
package nl.moj.server.feedback;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.CollectionUtil;
import nl.moj.server.util.HttpUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class FeedbackController {

    private final TeamRepository teamRepository;

    private final CompetitionRuntime competitionRuntime;

    @GetMapping("/feedback")
    public ModelAndView feedback(HttpServletRequest request) {
        CompetitionRuntime resultsProvider = competitionRuntime;

        if (HttpUtil.hasParam("competition")) {
            Long competitionId = Long.parseLong(HttpUtil.getParam("competition","1"));
            if (competitionRuntime.getActiveCompetitionsMap().containsKey(competitionId)) {
                Competition competition = competitionRuntime.getActiveCompetitionsMap().get(competitionId).getCompetition();
                resultsProvider = competitionRuntime.selectCompetitionRuntimeForGameStart(competition);
            }
        }

        ModelAndView model = new ModelAndView("testfeedback");
        List<Team> allTeams = teamRepository.findAllByRole(Role.USER);
        orderTeamsByName(allTeams, resultsProvider);

        List<List<Team>> partitionedTeams = CollectionUtil.partition(allTeams, 3);
        model.addObject("teams1", partitionedTeams.get(0));
        model.addObject("teams2", partitionedTeams.get(1));
        model.addObject("teams3", partitionedTeams.get(2));

        List<String> testNames = new ArrayList<>();

        if (resultsProvider.getCurrentAssignment() != null) {
            ActiveAssignment state = resultsProvider.getActiveAssignment();

            testNames = state.getTestNames();

            model.addObject("uuid", state.getAssignment().getUuid().toString());
            model.addObject("assignment", state.getAssignmentDescriptor().getDisplayName());
            model.addObject("timeLeft", state.getTimeRemaining());
            model.addObject("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addObject("running", state.isRunning());
        } else {
            model.addObject("assignment", "-");
            model.addObject("timeLeft", 0);
            model.addObject("time", 0);
            model.addObject("running", false);
        }
        model.addObject("submitLinks", request.isUserInRole("GAME_MASTER"));
        model.addObject("tests", testNames);
        model.addObject("competitionName", competitionRuntime.getCompetition().getDisplayName());

        return model;
    }

    @GetMapping(value = "/feedback/solution/{assignment}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    public @ResponseBody
    Submission getAssignmentSolution(@PathVariable("assignment") UUID assignment) {
        return Submission.builder()
                .files(competitionRuntime.getSolutionFiles(assignment).stream()
                        .map(f -> FileSubmission.builder()
                                .uuid(f.getUuid())
                                .filename(f.getShortName())
                                .content(f.getContentAsString())
                                .location(f.getAbsoluteFile().toString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping(value = "/feedback/solution/{assignment}/team/{team}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    public @ResponseBody
    Submission getSubmission(@PathVariable("assignment") UUID assignment, @PathVariable("team") UUID uuid) {
        return Submission.builder()
                .team(uuid)
                .files(competitionRuntime.getTeamSolutionFiles(assignment, teamRepository.findByUuid(uuid)).stream()
                        .filter(f -> f.getFileType() == AssignmentFileType.EDIT)
                        .map(f -> FileSubmission.builder()
                                .uuid(f.getUuid())
                                .filename(f.getShortName())
                                .content(f.getContentAsString())
                                .location(f.getAbsoluteFile().toString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private void orderTeamsByName(List<Team> allTeams, CompetitionRuntime resultsProvider) {
        if (resultsProvider.getCompetitionSession() != null) {
            allTeams.sort(Comparator.comparing(Team::getName));
        }
    }
}
