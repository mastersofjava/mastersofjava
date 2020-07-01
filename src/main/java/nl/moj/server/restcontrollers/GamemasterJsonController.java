package nl.moj.server.restcontrollers;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.teams.model.Role;
import nl.moj.server.util.HttpUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private final CompetitionRepository competitionRepository;

    private final CompetitionSessionRepository competitionSessionRepository;

    private final GamemasterTableComponents gamemasterTableComponents;

    private final SessionRegistry sessionRegistry;

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
            response.put("activeAssignment", activeAssignment.getAssignment());
            response.put("assignmentMetadata", activeAssignment.getAssignmentDescriptor());
        } else
        if (activeAssignment != null) {
            response.put("activeAssignment.getCompetitionSession", activeAssignment.getCompetitionSession());
        }
        response.put("principals, from session registry", sessionRegistry.getAllPrincipals());
        HttpServletRequest request = HttpUtil.getCurrentHttpRequest();

        List<String> nameList = Collections.list(request.getSession().getAttributeNames());
        response.put("session", nameList);

        for (String name : nameList) {
            response.put("session."+name, request.getSession().getAttribute(name));
            response.put("session.name."+name, request.getSession().getAttribute(name).getClass().getName());
        }

        SecurityContextImpl context = (SecurityContextImpl)request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        response.put("session.user.name", HttpUtil.getCurrentHttpRequestUserName());
        response.put("session.user.principal", context.getAuthentication().getPrincipal());
        response.put("session.user.selectedSession", HttpUtil.getSelectedUserSession(null));
        response.put("activeCompetitions", competitionRuntime.getActiveCompetitionsMap());
        response.put("activeCompetition", competitionRuntime.getCompetition());
        response.put("activeCompetitionSession", competitionRuntime.getCompetitionSession().getUuid());

        return response;
    }
    @GetMapping(value = "/availableCompetitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    List<Competition> getAvailableCompetitions() {
        List<Competition> listAll = competitionRepository.findAll();
        List<Competition> result = new ArrayList<>();
        for (Competition competition : listAll) {
            List<CompetitionSession>  sessions = competitionSessionRepository.findByCompetition(competition);

            for (CompetitionSession session: sessions) {
                if (session.isActive()) {
                    result.add(competition);
                }
            }
        }
        return result;
    }
    @GetMapping(value = "/runningCompetitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    Map<Long,String> getRunningCompetitions() {
        return competitionRuntime.getRunningCompetitionsQuickviewMap();
    }
    @GetMapping(value = "/systemState", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    Map<Long, CompetitionRuntime.CompetitionExecutionModel> getSystemState() {
        return competitionRuntime.getActiveCompetitionsMap();
    }
}
