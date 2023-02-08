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
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.authorization.Role;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.feedback.model.TeamFeedback;
import nl.moj.server.feedback.service.FeedbackService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.CollectionUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CompetitionRuntime competitionRuntime;

    @GetMapping("/feedback")
    @Transactional(Transactional.TxType.REQUIRED)
    public ModelAndView feedback(HttpServletRequest request) {
        ModelAndView model = new ModelAndView("testfeedback");
        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Assignment assignment = state.getAssignment();
        UUID sessionId = competitionRuntime.getSessionId();

        // TODO this should be fixed once competition runtime gets cleaned up.
        List<TeamFeedback> assignmentFeedback = feedbackService.getAssignmentFeedback(assignment != null ? assignment.getUuid() : null, sessionId);
        orderTeamsByName(assignmentFeedback);

        List<List<TeamFeedback>> partitionedTeams = CollectionUtil.partition(assignmentFeedback, 3);
        model.addObject("teams1", partitionedTeams.get(0));
        model.addObject("teams2", partitionedTeams.get(1));
        model.addObject("teams3", partitionedTeams.get(2));

        List<UUID> testIds = new ArrayList<>();
        if (state.isRunning()) {
            testIds = state.getTestUuids();
            model.addObject("uuid", state.getAssignment().getUuid().toString());
            model.addObject("assignment", state.getAssignmentDescriptor().getDisplayName());
            model.addObject("timeLeft", state.getSecondsRemaining());
            model.addObject("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addObject("running", state.isRunning());
        } else {
            model.addObject("assignment", "-");
            model.addObject("timeLeft", 0);
            model.addObject("time", 0);
            model.addObject("running", false);
        }
        model.addObject("submitLinks", request.isUserInRole("GAME_MASTER"));
        model.addObject("tests", testIds);
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
    Submission getSubmission(@PathVariable("assignment") UUID assignment, @PathVariable("team") UUID team) {
        return Submission.builder()
                .team(team)
                .files(competitionRuntime.getTeamSolutionFiles(team,competitionRuntime.getSessionId(),assignment).stream()
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

    private void orderTeamsByName(List<TeamFeedback> allTeams) {
        allTeams.sort(Comparator.comparing(t -> t.getTeam().getName()));
    }
}
