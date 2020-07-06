package nl.moj.server.restcontrollers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.competition.service.GamemasterTableComponents;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.JavaAssignmentFileResolver;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * delivering finegrained GameMaster data to the admin (can be extended)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GamemasterJsonController {

    private final MojServerProperties mojServerProperties;

    private final CompetitionRuntime competitionRuntime;

    private final CompetitionService competitionService;

    private final GamemasterTableComponents gamemasterTableComponents;

    private final SessionRegistry sessionRegistry;

    private final TaskScheduler taskScheduler;

    private final TeamRepository teamRepository;

    private final SubmitService submitService;

    private final AssignmentService assignmentService;

    @Autowired
    private Environment env;

    private boolean isEnvironmentForDevelopment() {
        return Arrays.asList(env.getActiveProfiles()).contains("local");
    }

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
        response.put("competitionstatus", gamemasterTableComponents.createAssignmentStatusMap());
        response.put("isCompetitionMode", isCompetitionMode);

        if (isCompetitionMode) {
            response.put("activeAssignment", activeAssignment.getAssignment());
            response.put("assignmentMetadata", activeAssignment.getAssignmentDescriptor());
        } else if (activeAssignment != null) {
            response.put("activeAssignment.getCompetitionSession", activeAssignment.getCompetitionSession());
        }
        response.put("principals, from session registry", sessionRegistry.getAllPrincipals());
        HttpServletRequest request = HttpUtil.getCurrentHttpRequest();

        List<String> nameList = Collections.list(request.getSession().getAttributeNames());
        response.put("session", nameList);

        for (String name : nameList) {
            response.put("session." + name, request.getSession().getAttribute(name));
            response.put("session.name." + name, request.getSession().getAttribute(name).getClass().getName());
        }

        SecurityContextImpl context = (SecurityContextImpl) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        response.put("httpsession.user.name", HttpUtil.getCurrentHttpRequestUserName());
        response.put("httpsession.user.principal", context.getAuthentication().getPrincipal());
        response.put("httpsession.user.selectedSession", HttpUtil.getSelectedUserSession(null));
        response.put("activeCompetition", competitionRuntime.getCompetition());
        response.put("activeCompetitionSession", competitionRuntime.getCompetitionSession().getUuid());

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
    @GetMapping(value = "/admin/clearRuntime", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<Long, ?> doClearRuntime() {
        List<Competition> competitionList = competitionService.getAvailableCompetitions();

        log.info("competitionList " + competitionList.stream().map(Competition::getShortName).collect(Collectors.toList()));
        for (Competition competition : competitionList) {
            boolean isActive = competitionRuntime.getActiveCompetitionsMap().containsKey(competition.getId());
            log.info("isActive " + competition.getShortName() + " " + isActive);
            if (!isActive) {
                competitionRuntime.loadMostRecentSession(competition);
            }
            Assert.isTrue(competitionRuntime.getActiveCompetitionsMap().containsKey(competition.getId()), "competition not loaded: "+competition.getId());
        }
        for (Competition competition : competitionList) {
           // CompetitionRuntime miniRuntime = competitionRuntime.selectCompetitionRuntimeForGameStart(competition);
            CompetitionRuntime.CompetitionExecutionModel model = competitionRuntime.getActiveCompetitionsMap().get(competition.getId());
            log.info("start miniruntime: " + competition.getDisplayName() + " " + model.getRunningAssignmentName() + " " + model.getTimer());
            model.getAssignmentExecutionModel().clearHandlers();
        }
        ThreadPoolTaskScheduler xScheduler = (ThreadPoolTaskScheduler)this.taskScheduler;
        log.info("xScheduler " +xScheduler);

        ScheduledThreadPoolExecutor xService = (ScheduledThreadPoolExecutor)xScheduler.getScheduledExecutor();
        log.info("xService " +xService);

        BlockingQueue<Runnable> queue = xService.getQueue();
        log.info("queue " +queue);
        Object[] scheduledJobs = queue.toArray();

        log.info("scheduledJobs " + Arrays.asList(scheduledJobs).size() + " " +Arrays.asList(scheduledJobs));

        return getRunningCompetitions();
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

    public class PerformanceValidation {
        private Map<String, Object> result = new TreeMap<>();
        private List<Team> teamList;
        int errorsCompile = 0;
        int errorsTest = 0;
        private Date startTime = new Date();
        private int runs;
        private String assignmentName;
        private List<AssignmentFile> fileList = new ArrayList<>();
        private SourceMessage createCodeInputInitial() {
            SourceMessage message = new SourceMessage();
            CompetitionRuntime.CompetitionExecutionModel model = competitionRuntime.getCompetitionModel();
            message = new SourceMessage();
            message.setAssignmentName(model.getRunningAssignmentName());
            message.setTests(new ArrayList<>());
            message.setSources(new TreeMap<>());
            for (AssignmentFile file: fileList) {
                if (file.getName().toLowerCase().contains("test")) {
                    message.getTests().add(file.getUuid().toString());
                } else
                if (!file.isReadOnly()) {
                    message.getSources().put(file.getUuid().toString(), file.getContentAsString());
                }
            }
            return message;
        }
        private SourceMessage createCodeInputWithSolution() {
            SourceMessage message = new SourceMessage();
            message = new SourceMessage();
            message.setAssignmentName(assignmentName);
            message.setTests(new ArrayList<>());
            message.setSources(new TreeMap<>());
            for (AssignmentFile file: fileList) {
                if (file.getName().toLowerCase().contains("test")) {
                    message.getTests().add(file.getUuid().toString());
                } else
                if (!file.isReadOnly()) {
                    JavaAssignmentFileResolver resolver = new JavaAssignmentFileResolver();
                    String path = file.getAbsoluteFile().toFile().getPath().replace("src\\main\\java","assets").replace("src/main/java","assets");
                    File solutionFile = new File(path.replace(".java","Solution.java"));

                    if (solutionFile.exists()) {
                        file = resolver.convertToAssignmentFile(file.getName(), solutionFile.toPath(), file.getBase(), solutionFile.toPath(), AssignmentFileType.EDIT, false, file.getUuid());
                    }
                    message.getSources().put(file.getUuid().toString(), file.getContentAsString());
                }
            }

            return message;
        }
        public PerformanceValidation(int runs) {
            result.put("timeStart", startTime.toString());
            this.runs = runs;
            teamList = teamRepository.findAllByRole(Role.USER);
            result.put("teams", teamList.size());
            result.put("runs", runs);
            CompetitionRuntime.CompetitionExecutionModel model = competitionRuntime.getCompetitionModel();
            assignmentName = model.getRunningAssignmentName();
            result.put("assignment", assignmentName);
            List<AssignmentFile> sourceList = assignmentService.getAssignmentFiles(competitionRuntime.getCurrentAssignment().getAssignment());

            for (AssignmentFile source : sourceList) {
                if (source.getFile().toFile().getName().endsWith(".java")) {
                    fileList.add(source);
                }
            }
            if (teamList.size()>maxUsers) {
                teamList = teamList.subList(0, maxUsers);
            }
        }

        public void doRuns() {

            for (int index =0; index< runs ;index++) {
                this.doOneRun();
            }
        }
        private int maxUsers = 100;
        private void doOneRun() {
            SourceMessage codeInputInitial = createCodeInputInitial();
            SourceMessage codeInputSolution =  createCodeInputWithSolution();
            boolean isOnlySolutionCompiliation = true;
            for (Team team: teamList) {
                try {
                    SubmitResult submitResult = submitService.compile(team, codeInputInitial).get(10, TimeUnit.SECONDS);
                    result.put("compile.problem", submitResult.isSuccess());
                } catch (Exception ex) {
                    errorsCompile++;
                    ex.printStackTrace();
                }

                try {
                    SubmitResult submitResult = submitService.compile(team, codeInputSolution).get(10, TimeUnit.SECONDS);
                    result.put("compileOk", submitResult.isSuccess());
                } catch (Exception ex) {
                    errorsCompile++;
                    ex.printStackTrace();
                }
                try {
                    SubmitResult submitResult = submitService.test(team, codeInputSolution).get(10, TimeUnit.SECONDS);
                    result.put("test", submitResult.isSuccess());
                } catch (Exception ex) {
                    errorsTest++;
                    ex.printStackTrace();
                }
            }

            result.put("errorsCompile", errorsCompile);
            result.put("errorsTest", errorsTest);
        }

        public Map<String, Object> getResult() {
            result.put("timeEnd", new Date().getTime() - startTime.getTime());
            return result;
        }
    }

    @GetMapping(value = "/admin/executeAgents", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<String, Object> doExecuteAgents() {
        Assert.isTrue(isEnvironmentForDevelopment(),"unauthorized");
        Map<Long, String> competitions = getRunningCompetitions();
        Assert.isTrue(!competitions.isEmpty(),"unauthorized");

        PerformanceValidation performanceValidation = new PerformanceValidation(10);
        performanceValidation.doRuns();
        return performanceValidation.getResult();
    }


}
