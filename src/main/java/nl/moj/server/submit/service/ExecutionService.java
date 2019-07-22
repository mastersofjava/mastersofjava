package nl.moj.server.submit.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.ExecutionModel;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.service.TestResults;
import nl.moj.server.test.service.TestService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExecutionService {

    private final CompetitionRuntime competition;
    private final CompileService compileService;
    private final TestService testService;
    private final Executor sequential;
    private final Executor parallel;

    public ExecutionService(@Qualifier("sequential") Executor sequential, @Qualifier("parallel") Executor parallel, CompetitionRuntime competition,
                            CompileService compileService, TestService testService) {
        this.sequential = sequential;
        this.parallel = parallel;
        this.competition = competition;
        this.compileService = compileService;
        this.testService = testService;
    }

    public CompletableFuture<CompileResult> compile(Team team, SourceMessage message) {
        return compileService.scheduleCompile(team, message, getExecutor());
    }

    public CompletableFuture<TestResults> test(Team team, List<AssignmentFile> tests) {
        return testService.scheduleTests(team, tests, getExecutor());
    }

    private Executor getExecutor() {
        ActiveAssignment activeAssignment = competition.getActiveAssignment();
        if (activeAssignment == null) {
            return parallel;
        }
        if (activeAssignment.getExecutionModel() == ExecutionModel.SEQUENTIAL) {
            return sequential;
        }
        return parallel;
    }

}
