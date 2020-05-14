package nl.moj.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.login.SignupForm;
import nl.moj.server.runtime.AssignmentRuntime;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequiredArgsConstructor
public class GamemasterController {

    private static final Logger log = LoggerFactory.getLogger(nl.moj.server.TaskControlController.class);

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

    private final AssignmentRuntime assignmentRuntime;

    private final GamemasterTableComponents gamemasterTableComponents;

    private final CompetitionService competitionService;

    private static final String STATUS_OK = "\"OK\"";

    @GetMapping(value = "/actions/clearAssignments", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String clearAssignments(HttpServletResponse resp) {
        ensureSecurity(resp);
        log.warn("clearAssignments entered");
        competition.stopCurrentAssignment();
        competition.getCompetitionState().getCompletedAssignments().clear();
        assignmentStatusRepository.deleteAll();
        assignmentResultRepository.deleteAll();
        return STATUS_OK;
    }

    @GetMapping(value = "/actions/startAssignment/{path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String startTask(@PathVariable("path") String pathname, HttpServletResponse resp) {
        ensureSecurity(resp);
        OrderedAssignment currentAssignment = competition.getCurrentAssignment();
        String result = STATUS_OK;
        if (currentAssignment == null) {
            competition.startAssignment(pathname);
        } else if (competition.getActiveAssignment().getTimeRemaining() == 0) {// stop finished assignment first
            competition.stopCurrentAssignment();
            competition.startAssignment(pathname);
        } else {
            result = "\"current assignment: " + currentAssignment.getAssignment().getName() + "\"";
        }
        return result;
    }

    @GetMapping(value = "/actions/stopAssignment", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String stopTask(HttpServletResponse resp) {
        ensureSecurity(resp);
        competition.stopCurrentAssignment();
        return STATUS_OK;
    }

    @GetMapping(value = "/actions/importUsersForSimulation", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String importUsers(HttpServletResponse resp) {
        ensureSecurity(resp);
        clearAssignments(resp);

        if (teamService.getTeams().size() < 10) {
            for (int index = 0; index < 100; index++) {
                SignupForm form = new SignupForm();
                form.setName("simulation_team_"+index);
                form.setPassword(form.getName());
                form.setPasswordCheck(form.getName());
                form.setCompany("simulation");
                form.setCountry("Nederland");
                competitionService.createNewTeam(form);
            }
        }
        return STATUS_OK;
    }

    @GetMapping(value = "/getTeams", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    List<Team> getTeamsAsJson(HttpServletResponse resp) {
        ensureSecurity(resp);
        return teamRepository.findAll();
    }

    private void ensureSecurity(HttpServletResponse resp) {
        resp.addHeader("X-Frame-Options", "allow-from *");
        resp.addHeader("Access-Control-Allow-Origin", "*");
    }

    @GetMapping(value = "/getAssignments", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    List<AssignmentDescriptor> getAssignmentsAsJson(HttpServletResponse resp) {
        ensureSecurity(resp);
        return competition.getAssignmentInfo();
    }

    private ObjectMapper parser = new ObjectMapper();

    @GetMapping(value = "/getAssignmentStateList", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    String getAssignmentStatesAsJson(HttpServletResponse resp) throws JsonProcessingException {
        ensureSecurity(resp);
        return parser.writeValueAsString(gamemasterTableComponents.createAssignmentStatusMap());
    }

    @GetMapping(value = "/getGameMasterState", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    public @ResponseBody
    Map<String, Object> getAssignmentSolution() {
        Map<String, Object> response = new TreeMap<>();
        response.put("mojServerProperties", mojServerProperties);
        response.put("assignments", gamemasterTableComponents.createAssignmentStatusMap());
        response.put("teams", teamRepository.findAll());


        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        if (activeAssignment != null && activeAssignment.getAssignment() != null) {
            response.put("activeAssignment.assignment", activeAssignment.getAssignment());
            response.put("activeAssignment.assignmentDescriptor", activeAssignment.getAssignmentDescriptor());

            List<Team> teams = teamService.getTeams();
            if (!teams.isEmpty()) {
                List<AssignmentFile> files = teamService.getTeamAssignmentFiles(competition.getCompetitionSession(), activeAssignment.getAssignment(), teams.get(0));
                response.put("activeAssignment.files", files);
            }
        }

        return response;
    }


}
