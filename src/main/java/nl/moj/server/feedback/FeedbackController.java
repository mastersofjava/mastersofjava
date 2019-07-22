package nl.moj.server.feedback;

import lombok.RequiredArgsConstructor;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.CollectionUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class FeedbackController {

    private final TeamRepository teamRepository;

    private final CompetitionRuntime competition;

    @GetMapping("/feedback")
    public ModelAndView feedback(HttpServletRequest request) {
        ModelAndView model = new ModelAndView("testfeedback");
        List<Team> allTeams = teamRepository.findAllByRole(Role.USER);
        orderTeamsByName(allTeams);

        List<List<Team>> partitionedTeams = CollectionUtil.partition(allTeams, 3);
        model.addObject("teams1", partitionedTeams.get(0));
        model.addObject("teams2", partitionedTeams.get(1));
        model.addObject("teams3", partitionedTeams.get(2));

        List<String> testNames = new ArrayList<>();

        if (competition.getCurrentAssignment() != null) {
            ActiveAssignment state = competition.getActiveAssignment();

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

        return model;
    }

    @GetMapping(value="/feedback/solution/{assignment}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER,Role.ADMIN})
    public @ResponseBody Submission getAssignmentSolution(@PathVariable("assignment") UUID assignment) {
        return Submission.builder()
                .files(competition.getSolutionFiles(assignment).stream()
                        .map(f -> FileSubmission.builder()
                                .uuid(f.getUuid())
                                .filename(f.getShortName())
                                .content(f.getContentAsString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping(value = "/feedback/solution/{assignment}/team/{team}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER,Role.ADMIN})
    public @ResponseBody
    Submission getSubmission(@PathVariable("assignment") UUID assignment, @PathVariable("team") UUID uuid) {
        return Submission.builder()
                .team(uuid)
                .files(competition.getTeamSolutionFiles(assignment, teamRepository.findByUuid(uuid)).stream()
                        .filter(f -> f.getFileType() == AssignmentFileType.EDIT)
                        .map(f -> FileSubmission.builder()
                                .uuid(f.getUuid())
                                .filename(f.getShortName())
                                .content(f.getContentAsString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private void orderTeamsByName(List<Team> allTeams) {
        if (competition.getCompetitionSession() != null) {
            allTeams.sort(Comparator.comparing(Team::getName));
        }
    }
}
