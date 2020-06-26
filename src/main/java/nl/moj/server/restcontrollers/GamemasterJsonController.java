package nl.moj.server.restcontrollers;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.teams.model.Role;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
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
            response.put("currentAssignment", activeAssignment.getAssignment());
            response.put("assignmentMetadata", activeAssignment.getAssignmentDescriptor());
        }
        response.put("principals, from session registry", sessionRegistry.getAllPrincipals());
        HttpServletRequest request = getCurrentHttpRequest();

        List<String> nameList = Collections.list(request.getSession().getAttributeNames());
        response.put("session", nameList);

        for (String name : nameList) {
            response.put("session."+name, request.getSession().getAttribute(name));
            response.put("session.name."+name, request.getSession().getAttribute(name).getClass().getName());
        }

        SecurityContextImpl context = (SecurityContextImpl)request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        response.put("session.user.name", context.getAuthentication().getName());
        response.put("session.user.principal", context.getAuthentication().getPrincipal());

        return response;
    }
    public static String getParam(String param) {
        HttpServletRequest req = getCurrentHttpRequest();
        return req==null?null: req.getParameter(param);
    }
    public static String getCurrentHttpRequestUserName() {
        SecurityContextImpl context = (SecurityContextImpl)getCurrentHttpRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        return context.getAuthentication().getName();
    }
    public static HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }

}
