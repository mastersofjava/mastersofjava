package nl.moj.worker.java;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.messages.*;
import nl.moj.server.runtime.service.RuntimeService;
import nl.moj.server.submit.service.ExecutionService;
import nl.moj.server.util.CompletableFutures;
import nl.moj.worker.WorkerService;
import nl.moj.worker.java.compile.CompileOutput;
import nl.moj.worker.java.compile.CompileRunnerService;
import nl.moj.worker.java.test.TestOutput;
import nl.moj.worker.java.test.TestRunnerService;
import nl.moj.worker.workspace.Workspace;
import nl.moj.worker.workspace.WorkspaceService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class JavaService {

    private final CompileRunnerService compileRunnerService;
    private final TestRunnerService testRunnerService;
    private final RuntimeService runtimeService;
    private final WorkspaceService workspaceService;
    private final ExecutionService executionService;
    private final WorkerService workerService;

    public CompletableFuture<JMSCompileResponse> compile(JMSCompileRequest compileRequest) {
        UUID runId = UUID.randomUUID();
        try {
            AssignmentDescriptor ad = runtimeService.getAssignmentDescriptor(compileRequest.getAssignment());
            Workspace workspace = workspaceService.getWorkspace(ad, compileRequest.getSources());
            return compile(workspace, runId)
                    .thenApply(co -> {
                        closeWorkspace(workspace);
                        return toCompileResponse(compileRequest.getAttempt(), co);
                    });

        } catch (Exception e) {
            return CompletableFuture.completedFuture(JMSCompileResponse.builder()
                    .runId(runId)
                    .worker(workerService.getWorkerIdentification())
                    .attempt(compileRequest.getAttempt())
                    .started(Instant.now())
                    .ended(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build());
        }
    }


    public CompletableFuture<JMSTestResponse> test(JMSTestRequest testRequest) {
        UUID runId = UUID.randomUUID();
        try {
            AssignmentDescriptor ad = runtimeService.getAssignmentDescriptor(testRequest.getAssignment());
            Workspace workspace = workspaceService.getWorkspace(ad, testRequest.getSources());
            return compile(workspace, runId)
                    .thenCompose(co -> {
                        if (co.isSuccess()) {
                            return test(workspace, testRequest.getTests(), runId)
                                    .thenApply(r -> toTestResponse(testRequest.getAttempt(), runId, co, r));
                        } else {
                            return CompletableFuture.completedFuture(JMSTestResponse
                                    .builder()
                                    .runId(runId)
                                    .worker(workerService.getWorkerIdentification())
                                    .attempt(testRequest.getAttempt())
                                    .started(co.getDateTimeStart())
                                    .ended(Instant.now())
                                    .aborted(false)
                                    .compileResponse(toCompileResponse(null, co))
                                    .build());
                        }
                    })
                    .thenApply(to -> {
                        closeWorkspace(workspace);
                        return to;
                    });

        } catch (Exception e) {
            return CompletableFuture.completedFuture(JMSTestResponse.builder()
                    .runId(runId)
                    .worker(workerService.getWorkerIdentification())
                    .attempt(testRequest.getAttempt())
                    .started(Instant.now())
                    .ended(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build());
        }
    }

    public CompletableFuture<JMSSubmitResponse> submit(JMSSubmitRequest submitRequest) {
        return CompletableFuture.completedFuture(JMSSubmitResponse.builder().aborted(true).reason("Not Implemented").build());
    }

    private JMSCompileResponse toCompileResponse(UUID attempt, CompileOutput co) {
        return JMSCompileResponse.builder()
                .worker(workerService.getWorkerIdentification())
                .success(co.isSuccess())
                .attempt(attempt)
                .started(co.getDateTimeStart())
                .ended(co.getDateTimeEnd())
                .output(concat(co.getOutput(), co.getErrorOutput()))
                .timeout(co.isTimedOut())
                .aborted(co.isAborted())
                .reason(co.getReason())
                .build();
    }

    private JMSTestResponse toTestResponse(UUID attempt, UUID runId, CompileOutput co, List<TestOutput> r) {
        return JMSTestResponse.builder()
                .runId(runId)
                .worker(workerService.getWorkerIdentification())
                .attempt(attempt)
                .started(co.getDateTimeStart())
                .ended(Instant.now())
                .aborted(false)
                .compileResponse(toCompileResponse(null, co))
                .testCaseResults(r.stream().map(this::toTestCaseResult).toList())
                .build();
    }

    private JMSTestCaseResult toTestCaseResult(TestOutput to) {
        return JMSTestCaseResult.builder()
                .runId(to.getRunId())
                .worker(workerService.getWorkerIdentification())
                .testCase(to.getTestCase())
                .aborted(to.isAborted())
                .reason(to.getReason())
                .success(to.isSuccess())
                .started(to.getDateTimeStart())
                .ended(to.getDateTimeEnd())
                .timeout(to.isTimedOut())
                .output(concat(to.getOutput(), to.getErrorOutput()))
                .build();
    }

    private static void closeWorkspace(Workspace workspace) {
        try {
            workspace.close();
        } catch (Exception ex) {
            log.error("Failed to close workspace {}, ignoring.", workspace.getRoot());
        }
    }

    private CompletableFuture<List<TestOutput>> test(Workspace workspace, List<JMSTestCase> testCases, UUID runId) {
        AssignmentDescriptor ad = workspace.getAssignmentDescriptor();
        List<CompletableFuture<TestOutput>> tests = new ArrayList<>();
        testCases.forEach(tc ->
                tests.add(CompletableFuture.supplyAsync(() -> testRunnerService.test(workspace, tc, runId),
                        executionService.getExecutor(ad))));
        return CompletableFutures.allOf(tests);
    }

    private CompletableFuture<CompileOutput> compile(Workspace workspace, UUID runId) {
        return CompletableFuture.supplyAsync(() -> compileRunnerService.compile(workspace, runId),
                executionService.getExecutor(workspace.getAssignmentDescriptor()));
    }

    private String concat(String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && b.length() > 0) {
            sb.append(a);
        }
        if (sb.length() > 0 && b != null && b.length() > 0) {
            sb.append("\n\n");
        }
        if (b != null && b.length() > 0) {
            sb.append(b);
        }
        return sb.toString();
    }
}
