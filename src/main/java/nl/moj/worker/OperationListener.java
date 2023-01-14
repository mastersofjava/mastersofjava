package nl.moj.worker;

import java.time.Instant;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.JMSCompileRequest;
import nl.moj.common.messages.JMSCompileResponse;
import nl.moj.common.messages.JMSTestRequest;
import nl.moj.common.messages.JMSTestResponse;
import nl.moj.worker.java.JavaService;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperationListener {

    private final JmsTemplate jmsTemplate;
    private final JavaService javaService;
    private final WorkerService workerService;

    private final Tracer tracer;

    @JmsListener(destination = "compile_request")
    @NewSpan
    public void receiveCompileRequest(JMSCompileRequest compileRequest) {
        String traceId = traceId();
        try {
            log.info("Received compile attempt request {}", compileRequest.getAttempt());
            javaService.compile(compileRequest, traceId).thenApply(cr -> {
                log.info("Compile attempt request {} finished with {}", cr.getAttempt(), cr);
                try {
                    jmsTemplate.convertAndSend("compile_response", cr);
                } catch (Throwable t) {
                    log.error("FAIL", t);
                }
                return cr;
            });
        } catch (Exception e) {
            log.error("Compile failed for attempt {}", compileRequest.getAttempt(), e);
            try {
                jmsTemplate.convertAndSend("compile_response", JMSCompileResponse.builder()
                        .attempt(compileRequest.getAttempt())
                        .ended(Instant.now())
                        .started(Instant.now())
                        .aborted(true)
                        .reason(e.getMessage())
                        .success(false)
                        .timeout(false)
                        .worker(workerService.getWorkerIdentification())
                        .traceId(traceId)
                        .build());
            } catch (Throwable t) {
                log.error("FAIL", t);
            }

        }
    }

    @JmsListener(destination = "test_request")
    @NewSpan
    public void receiveTestRequest(JMSTestRequest testRequest) {
        String traceId = traceId();
        try {
            log.info("Received test attempt request {}", testRequest.getAttempt());
            javaService.test(testRequest, traceId).thenApply(tr -> {
                log.info("Test attempt request {} finished with {}", tr.getAttempt(), tr);
                try {
                    jmsTemplate.convertAndSend("test_response", tr);
                } catch (Throwable t) {
                    log.error("FAIL", t);
                }
                return tr;
            });
        } catch (Exception e) {
            log.error("Test failed for attempt {}", testRequest.getAttempt(), e);
            try {
                jmsTemplate.convertAndSend("test_response", JMSTestResponse.builder()
                        .attempt(testRequest.getAttempt())
                        .ended(Instant.now())
                        .started(Instant.now())
                        .aborted(true)
                        .reason(e.getMessage())
                        .traceId(traceId)
                        .worker(workerService.getWorkerIdentification())
                        .build());
            } catch (Throwable t) {
                log.error("FAIL", t);
            }
        }
    }

    private String traceId() {
        return Objects.requireNonNull(tracer.currentSpan()).context().traceId();
    }
}
