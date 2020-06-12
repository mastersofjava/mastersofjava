package nl.moj.server.restcontrollers;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.teams.model.Role;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.security.RolesAllowed;
import java.util.Map;
import java.util.TreeMap;

/**
 * delivering finegrained GameMaster data to the admin (can be extended)
 */
@Controller
@RequiredArgsConstructor
public class GamemasterJsonController {

    private final MojServerProperties mojServerProperties;

    private final CompetitionRuntime competitionRuntime;

    private final GamemasterTableComponents gamemasterTableComponents;

    @GetMapping(value = "/getGameMasterState", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    public @ResponseBody
    Map<String, Object> getAdminDomainIntroduction() {
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment();
        boolean isCompetitionMode = activeAssignment != null && activeAssignment.getAssignment() != null;

        Map<String, Object> response = new TreeMap<>();
        response.put("application.yaml", mojServerProperties);
        response.put("java.home", System.getProperties().get("java.home"));
        response.put("competitionstatus", gamemasterTableComponents.createAssignmentStatusMap());
        response.put("isCompetitionMode", isCompetitionMode);

        if (isCompetitionMode) {
            response.put("currentAssignment", activeAssignment.getAssignment());
            response.put("assignmentMetadata", activeAssignment.getAssignmentDescriptor());
        }
        return response;
    }


}
