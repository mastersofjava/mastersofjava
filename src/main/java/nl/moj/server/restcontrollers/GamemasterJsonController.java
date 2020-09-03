package nl.moj.server.restcontrollers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.authorization.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * delivering finegrained GameMaster data to the admin (can be extended)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GamemasterJsonController {
    @Autowired
    private Environment env;

    private final MojServerProperties mojServerProperties;

    private final CompetitionRuntime competitionRuntime;

    private final CompetitionService competitionService;

    private final GamemasterTableComponents gamemasterTableComponents;

    private final SessionRegistry sessionRegistry;

    private final TaskScheduler taskScheduler;

    @GetMapping(value = "/getGameMasterState", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.GAME_MASTER, Role.ADMIN})
    public @ResponseBody
    Map<String, Object> getAdminDomainIntroduction() {
        ActiveAssignment activeAssignment = competitionRuntime.getActiveAssignment();
        boolean isCompetitionMode = activeAssignment != null && activeAssignment.getAssignment() != null;

        Map<String, Object> response = new TreeMap<>();
        response.put("application.yaml", mojServerProperties);
        response.put("java.home", System.getProperties().get("java.home"));
        response.put("spring.datasource.driver-class-name", env.getProperty("spring.datasource.driver-class-name"));
        response.put("spring.datasource.url", env.getProperty("spring.datasource.url"));
        response.put("isCompetitionMode", isCompetitionMode);
        return response;
    }

    @GetMapping(value = "/admin/availableCompetitions", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<Long, String> getAvailableCompetitions() {
        List<Competition> competitionList = competitionService.getAvailableCompetitions();

        Map<Long, String> result = new TreeMap<>();

        for (Competition competition: competitionList) {
            result.put(competition.getId(), competition.getDisplayName());
        }
        return result;
    }

    @GetMapping(value = "/admin/runningCompetitions", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<Long, String> getRunningCompetitions() {
        Map<Long, String> resultCached = competitionRuntime.getRunningCompetitionsQuickviewMap();
        Map<Long, String> resultDb = getAvailableCompetitions();
        List<Long> keys = new ArrayList<>();
        keys.addAll(resultCached.keySet());
        for (Long competitionId : keys) {
            if (!resultDb.containsKey(competitionId)) {
                resultCached.remove(competitionId);
            }
        }
        return resultCached;
    }
    @GetMapping(value = "/admin/runningAssignments", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<Long, String> getRunningAssignments() {
        Map<Long, String> resultCached = competitionRuntime.getRunningCompetitionsQuickviewMap();
        Map<Long, String> resultDb = getAvailableCompetitions();
        List<Long> keys = new ArrayList<>();
        keys.addAll(resultCached.keySet());
        for (Long competitionId : keys) {
            if (!resultDb.containsKey(competitionId)) {
                resultCached.remove(competitionId);
            }
        }
        return resultCached;
    }
    @GetMapping(value = "/admin/systemState", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<Long, CompetitionRuntime.CompetitionExecutionModel> getSystemState() {
        return competitionRuntime.getActiveCompetitionsMap();
    }
    @MessageMapping("/control/performanceValidation")
    @SendToUser("/queue/controlfeedback")
    @RolesAllowed({Role.ADMIN})
    public String doValidatePerformanceViaGui() {
        doValidatePerformance();
        return "beschikbare competities allen geactiveerd, reload pagina";
    }

    @GetMapping(value = "/admin/activateAllAvailable", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<Long, String> doValidatePerformance() {
        List<Competition> competitionList = competitionService.getAvailableCompetitions();
        try {
            log.info("competitionList " + competitionList.stream().map(Competition::getShortName).collect(Collectors.toList()));
            for (Competition competition : competitionList) {
                boolean isActive = competitionRuntime.getActiveCompetitionsMap().containsKey(competition.getId());
                log.info("isActive " + competition.getShortName() + " " + isActive);
                if (!isActive) {
                    competitionRuntime.loadMostRecentSession(competition);
                }
                Assert.isTrue(competitionRuntime.getActiveCompetitionsMap().containsKey(competition.getId()), "competition not loaded: "+competition.getId());
            }
            log.info("loadMostRecentSession.DONE Competition.IdList " + competitionRuntime.getActiveCompetitionsMap().keySet());

            for (Competition competition : competitionList) {
                log.info("start miniruntime: " + competition.getDisplayName());
                CompetitionRuntime miniRuntime = competitionRuntime.selectCompetitionRuntimeForGameStart(competition);
                CompetitionRuntime.CompetitionExecutionModel model = competitionRuntime.getActiveCompetitionsMap().get(competition.getId());
                boolean isRunning = model.getAssignmentExecutionModel().isRunning();
                log.info("miniRuntime.isRunning " + model.getCompetition().getName()+" " +model.getCompetitionSession().getUuid() +  " running " + isRunning + " running.db " + model.getCompetitionSession().isRunning());

                if (!isRunning) {
                    OrderedAssignment assignment = miniRuntime.determineNextAssignmentIfAny();
                    log.info("miniRuntime.startAssignment " + assignment);
                    if (assignment != null) {
                        miniRuntime.startAssignment(assignment.getAssignment().getName());
                    }
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
        }
        return getRunningCompetitions();
    }



}
