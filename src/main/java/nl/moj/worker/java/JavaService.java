package nl.moj.worker.java;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.messages.*;
import nl.moj.server.runtime.service.RuntimeService;
import nl.moj.server.util.CompletableFutures;
import nl.moj.worker.ExecutionService;
import nl.moj.worker.WorkerService;
import nl.moj.worker.java.compile.CompileOutput;
import nl.moj.worker.java.compile.CompileRunnerService;
import nl.moj.worker.java.test.TestCaseOutput;
import nl.moj.worker.java.test.TestOutput;
import nl.moj.worker.java.test.TestRunnerService;
import nl.moj.worker.workspace.Workspace;
import nl.moj.worker.workspace.WorkspaceService;
import org.springframework.stereotype.Service;

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

    public CompletableFuture<JMSCompileResponse> compile(JMSCompileRequest compileRequest, String traceId) {
        try {
            AssignmentDescriptor ad = runtimeService.getAssignmentDescriptor(compileRequest.getAssignment());
            Workspace workspace = workspaceService.getWorkspace(ad, compileRequest.getSources());
            return compile(workspace)
                    .thenApply(co -> {
                        closeWorkspace(workspace);
                        return toCompileResponse(compileRequest.getAttempt(), traceId, co);
                    });

        } catch (Exception e) {
            return CompletableFuture.completedFuture(JMSCompileResponse.builder()
                    .traceId(traceId)
                    .worker(workerService.getWorkerIdentification())
                    .attempt(compileRequest.getAttempt())
                    .started(Instant.now())
                    .ended(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build());
        }
    }

    public CompletableFuture<JMSTestResponse> test(JMSTestRequest testRequest, String traceId) {
        try {
            AssignmentDescriptor ad = runtimeService.getAssignmentDescriptor(testRequest.getAssignment());
            Workspace workspace = workspaceService.getWorkspace(ad, testRequest.getSources());
            return test(workspace, testRequest.getTests())
                    .thenApply(to -> {
                        closeWorkspace(workspace);
                        return toTestResponse(testRequest.getAttempt(), traceId, to);
                    });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(JMSTestResponse.builder()
                    .traceId(traceId)
                    .worker(workerService.getWorkerIdentification())
                    .attempt(testRequest.getAttempt())
                    .started(Instant.now())
                    .ended(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build());
        }

    }

    public CompletableFuture<JMSSubmitResponse> submit(JMSSubmitRequest submitRequest, String traceId) {
        try {
            AssignmentDescriptor ad = runtimeService.getAssignmentDescriptor(submitRequest.getAssignment());
            Workspace workspace = workspaceService.getWorkspace(ad, submitRequest.getSources());
            return test(workspace, submitRequest.getTests())
                    .thenApply(to -> {
                        closeWorkspace(workspace);
                        return toSubmitResponse(submitRequest.getAttempt(), traceId, to);
                    });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(JMSSubmitResponse.builder()
                    .traceId(traceId)
                    .worker(workerService.getWorkerIdentification())
                    .attempt(submitRequest.getAttempt())
                    .started(Instant.now())
                    .ended(Instant.now())
                    .aborted(true)
                    .reason(e.getMessage())
                    .build());
        }
    }

    private JMSCompileResponse toCompileResponse(UUID attempt, String traceId, CompileOutput co) {
        return JMSCompileResponse.builder()
                .traceId(traceId)
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

    private JMSTestResponse toTestResponse(UUID attempt, String traceId, TestOutput to) {
        return JMSTestResponse.builder()
                .traceId(traceId)
                .worker(workerService.getWorkerIdentification())
                .attempt(attempt)
                .started(to.getDateTimeStart())
                .ended(to.getDateTimeEnd())
                .aborted(false)
                .compileResponse(toCompileResponse(null, traceId, to.getCompileOutput()))
                .testCaseResults(to.getTestCases().stream().map(tcr -> toTestCaseResult(tcr, traceId)).toList())
                .build();
    }

    private JMSTestCaseResult toTestCaseResult(TestCaseOutput to, String traceId) {
        return JMSTestCaseResult.builder()
                .traceId(traceId)
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

    private JMSSubmitResponse toSubmitResponse(UUID attempt, String traceId, TestOutput to) {
        return JMSSubmitResponse.builder()
                .attempt(attempt)
                .traceId(traceId)
                .worker(workerService.getWorkerIdentification())
                .started(to.getDateTimeStart())
                .ended(to.getDateTimeEnd())
                .aborted(false)
                .testResponse(toTestResponse(null, traceId, to))
                .build();
    }

    private static void closeWorkspace(Workspace workspace) {
        try {
            workspace.close();
        } catch (Exception ex) {
            log.error("Failed to close workspace {}, ignoring.", workspace.getRoot());
        }
    }

    private CompletableFuture<TestOutput> test(Workspace workspace, List<JMSTestCase> testCases) {
        AssignmentDescriptor ad = workspace.getAssignmentDescriptor();
        return compile(workspace)
                .thenCompose(co -> {
                    if (co.isSuccess()) {
                        List<CompletableFuture<TestCaseOutput>> tests = new ArrayList<>();
                        testCases.forEach(tc ->
                                tests.add(CompletableFuture.supplyAsync(() -> testRunnerService.test(workspace, tc),
                                        executionService.getExecutor(ad))));
                        return CompletableFutures.allOf(tests).thenApply(tcs ->
                                TestOutput.builder()
                                        .compileOutput(co)
                                        .testCases(tcs)
                                        .dateTimeStart(co.getDateTimeStart())
                                        .dateTimeEnd(Instant.now())
                                        .build());
                    } else {
                        return CompletableFuture.completedFuture(TestOutput.builder()
                                .compileOutput(co)
                                .dateTimeStart(co.getDateTimeStart())
                                .dateTimeEnd(Instant.now())
                                .build());
                    }
                });
    }

    private CompletableFuture<CompileOutput> compile(Workspace workspace) {
        return CompletableFuture.supplyAsync(() -> compileRunnerService.compile(workspace),
                executionService.getExecutor(workspace.getAssignmentDescriptor()));
    }

    private String concat(String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && a.length() > 0) {
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
