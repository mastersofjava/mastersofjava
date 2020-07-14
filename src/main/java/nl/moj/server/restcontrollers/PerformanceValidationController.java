package nl.moj.server.restcontrollers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.JavaAssignmentFileResolver;
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
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.security.RolesAllowed;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * controller for performance validation
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PerformanceValidationController {
    @Autowired
    private Environment env;

    private final CompetitionRuntime competitionRuntime;

    private final TeamRepository teamRepository;

    private final SubmitService submitService;

    private final AssignmentService assignmentService;

    private boolean isEnvironmentForDevelopment() {
        return Arrays.asList(env.getActiveProfiles()).contains("local");
    }
    private AgentStartupCache agentStartupCache = new AgentStartupCache();

    /**
     * prepares available assignments and teams for jmeter execution
     */
    public class AgentStartupCache {
        private List<Team> teamList;
        private Map<String, List<AssignmentFile>> assignmentFilesCacheMap = new LinkedHashMap<>();
        private int  errorsTest=0;
        private int  timeoutsTest=0;
        private int  executionTest=0;
        private int  solutionErrors=0;
        private void ensureCacheTeamList() {
            if (teamList==null) {
                teamList = teamRepository.findAllByRole(Role.USER);
            }
        }
        private void resetStateForNewTestRun() {
            assignmentFilesCacheMap.clear();
            errorsTest=0;
            executionTest=0;
            timeoutsTest = 0 ;
            solutionErrors = 0;
        }
        private void ensureCacheForAssignments() {
            if (assignmentFilesCacheMap.containsKey(competitionRuntime.getCurrentAssignment().getAssignment().getName())) {
                // test run has already started
                return;
            }
            resetStateForNewTestRun();
            List<AssignmentFile> sourceList = assignmentService.getAssignmentFiles(competitionRuntime.getCurrentAssignment().getAssignment());
            List<AssignmentFile> fileList = new ArrayList<>();
            for (AssignmentFile source : sourceList) {
                if (source.getFile().toFile().getName().endsWith(".java")) {
                    fileList.add(source);
                }
            }
            assignmentFilesCacheMap.put(competitionRuntime.getCurrentAssignment().getAssignment().getName(), fileList);
        }

        public List<AssignmentFile> getAssignmentFiles() {
            return assignmentFilesCacheMap.get(competitionRuntime.getCurrentAssignment().getAssignment().getName());
        }
    }


    public class PerformanceValidation {
        private Map<String, Object> outputParameters = new TreeMap<>();
        private List<Team> inputTeamList;
        private int errorsTest = 0;
        private int selectedUser = -1;
        private Date startTime = new Date();
        private int runs;
        private String assignmentName;
        private List<AssignmentFile> fileList = new ArrayList<>();
        private CompetitionRuntime.CompetitionExecutionModel model;

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
            model = competitionRuntime.getCompetitionModel();
            assignmentName = model.getRunningAssignmentName();
            this.runs = runs;
            agentStartupCache.ensureCacheTeamList();
            agentStartupCache.ensureCacheForAssignments();
            fileList.addAll(agentStartupCache.getAssignmentFiles());
            model = competitionRuntime.getCompetitionModel();
            outputParameters.put("timeStart", startTime.toString());
            outputParameters.put("runs", runs);
            outputParameters.put("assignment", assignmentName);
        }

        public void setSelectedUser(int selectedUser) {
            this.selectedUser = selectedUser;
        }

        public void doRuns() {
            for (int index =0; index< runs ;index++) {
                this.doOneRun();
            }
        }

        public List<Team> getTeamInputList() {
            if (inputTeamList==null) {
                inputTeamList = new ArrayList<>();
                if (selectedUser==-1) {
                    if (agentStartupCache.teamList.size()>maxUsers) {
                        inputTeamList = agentStartupCache.teamList.subList(0, maxUsers);
                    } else {
                        inputTeamList = agentStartupCache.teamList;
                    }
                } else {
                    if (agentStartupCache.teamList.size()<selectedUser) {
                        selectedUser = 0;
                    }
                    Team team = agentStartupCache.teamList.get(selectedUser);

                    inputTeamList.add(team);
                }
            }
            outputParameters.put("inputTeams", inputTeamList.size());
            outputParameters.put("cachedTeams", agentStartupCache.teamList.size());
            return inputTeamList;
        }

        private int maxUsers = 100;
        private boolean doOneRun() {
            SourceMessage codeInputSolution =  createCodeInputWithSolution();
            for (Team team: getTeamInputList()) {
                try {
                    agentStartupCache.executionTest++;
                    SubmitResult submitResult = submitService.test(team, codeInputSolution).get(10, TimeUnit.SECONDS);
                    outputParameters.put("test", submitResult.isSuccess());
                    if (!submitResult.isSuccess()) {
                        agentStartupCache.solutionErrors++;
                        outputParameters.put("solutionErrors", agentStartupCache.solutionErrors);
                        errorsTest++;
                    }
                } catch (TimeoutException ex) {
                    errorsTest++;
                    agentStartupCache.timeoutsTest++;
                    agentStartupCache.errorsTest++;
                    log.error("timeout intercepted", ex);
                } catch (Exception ex) {
                    errorsTest++;
                    agentStartupCache.errorsTest++;
                    ex.printStackTrace();
                    log.error("error intercepted", ex);
                }
            }

            outputParameters.put("errorsTest", errorsTest);
            outputParameters.put("solutionErrors", agentStartupCache.solutionErrors);
            outputParameters.put("errorsTest.Total", agentStartupCache.errorsTest);
            outputParameters.put("errorsTimeout.Total", agentStartupCache.timeoutsTest);
            outputParameters.put("executionTest.Total", agentStartupCache.executionTest);
            return errorsTest==0;
        }

        public Map<String, Object> getOutputParameters() {
            outputParameters.put("timeEnd", new Date().getTime() - startTime.getTime());
            return outputParameters;
        }
    }

    @GetMapping(value = "/admin/executeAgents", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Role.ADMIN})
    public @ResponseBody
    Map<String, Object> doExecuteAgents() {
        Assert.isTrue(isEnvironmentForDevelopment(),"unauthorized");
        Assert.isTrue(competitionRuntime.getCurrentAssignment()!=null,"unready");

        PerformanceValidation performanceValidation = new PerformanceValidation(10);
        performanceValidation.doRuns();
        return performanceValidation.getOutputParameters();
    }

    /**
     * JMeter entrypoint, only available on localhost with profile local.
    */
    @GetMapping(value = "/admin/executeAgent", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    Map<String, Object> doExecuteOneAgentByJMeter() {
        Assert.isTrue(isEnvironmentForDevelopment(),"unauthorized");// nb deze methode is aanroepbaar via JMeter, alleen op localhost.
        Assert.isTrue(competitionRuntime.getCurrentAssignment()!=null,"unready");
        int nr = Integer.parseInt(HttpUtil.getParam("agent","0"));
        PerformanceValidation performanceValidation = new PerformanceValidation(1);
        performanceValidation.setSelectedUser(nr);

        if (!performanceValidation.doOneRun()) {
            throw new ForbiddenException();// this ensures that the error is registrated in JMeter
        }

        if (HttpUtil.hasParam("reset_cache")) { // explicit reset of rundata by admin
            agentStartupCache = new AgentStartupCache();
            performanceValidation.getOutputParameters().put("reset_cache","reset_cache");
        }
        return performanceValidation.getOutputParameters();
    }
}
