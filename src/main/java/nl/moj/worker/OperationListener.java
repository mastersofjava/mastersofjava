package nl.moj.worker;

import java.time.Instant;
import java.util.Objects;

import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.JMSCompileRequest;
import nl.moj.common.messages.JMSCompileResponse;
import nl.moj.common.messages.JMSRequest;
import nl.moj.common.messages.JMSSubmitRequest;
import nl.moj.common.messages.JMSSubmitResponse;
import nl.moj.common.messages.JMSTestRequest;
import nl.moj.common.messages.JMSTestResponse;
import nl.moj.worker.java.JavaService;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperationListener {
    private static final String REQUEST_DESTINATION = "operation_request";
    private static final String RESPONSE_DESTINATION = "operation_response";

    private final JmsTemplate jmsTemplate;
    private final JavaService javaService;
    private final WorkerService workerService;

    private final Tracer tracer;

    @JmsListener(destination = REQUEST_DESTINATION)
    @NewSpan
    public void receiveOperationRequest(JMSRequest request) {
        log.info("On-Thread: {}-{}", workerService.getWorkerIdentification(), Thread.currentThread().getName());
        if (request instanceof JMSCompileRequest r) {
            receiveCompileRequest(r);
        } else if (request instanceof JMSTestRequest r) {
            receiveTestRequest(r);
        } else if (request instanceof JMSSubmitRequest r) {
            receiveSubmitRequest(r);
        } else {
            log.warn("Unable to receive operation request of type {}, ignoring.", request.getClass().getName());
        }
    }

    private void receiveCompileRequest(JMSCompileRequest compileRequest) {
        String traceId = traceId();
        try {
            log.info("Received compile attempt {}", compileRequest.getAttempt());
            javaService.compile(compileRequest, traceId).thenApply(cr -> {
                log.info("Compile attempt {} finished with {}", cr.getAttempt(), cr);
                try {
                    jmsTemplate.convertAndSend(RESPONSE_DESTINATION, cr);
                } catch (Throwable t) {
                    log.error("FAIL", t);
                }
                return cr;
            }).join();
        } catch (Exception e) {
            log.error("Compile failed for attempt {}", compileRequest.getAttempt(), e);
            try {
                jmsTemplate.convertAndSend(RESPONSE_DESTINATION, JMSCompileResponse.builder()
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

    private void receiveTestRequest(JMSTestRequest testRequest) {
        String traceId = traceId();
        try {
            log.info("Received test attempt {}", testRequest.getAttempt());
            javaService.test(testRequest, traceId).thenApply(tr -> {
                log.info("Test attempt {} finished with {}", tr.getAttempt(), tr);
                try {
                    jmsTemplate.convertAndSend(RESPONSE_DESTINATION, tr);
                } catch (Throwable t) {
                    log.error("FAIL", t);
                }
                return tr;
            }).join();
        } catch (Exception e) {
            log.error("Test failed for attempt {}", testRequest.getAttempt(), e);
            try {
                jmsTemplate.convertAndSend(RESPONSE_DESTINATION, JMSTestResponse.builder()
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

    private void receiveSubmitRequest(JMSSubmitRequest submitRequest) {
        String traceId = traceId();
        try {
            log.info("Received submit attempt {}", submitRequest.getAttempt());
            javaService.submit(submitRequest, traceId).thenApply(tr -> {
                log.info("Submit attempt {} finished with {}", tr.getAttempt(), tr);
                try {
                    jmsTemplate.convertAndSend(RESPONSE_DESTINATION, tr);
                } catch (Throwable t) {
                    log.error("FAIL", t);
                }
                return tr;
            }).join();
        } catch (Exception e) {
            log.error("Submit failed for attempt {}", submitRequest.getAttempt(), e);
            try {
                jmsTemplate.convertAndSend(RESPONSE_DESTINATION, JMSSubmitResponse.builder()
                        .attempt(submitRequest.getAttempt())
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
