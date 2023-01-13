package nl.moj.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.JMSCompileRequest;
import nl.moj.common.messages.JMSCompileResponse;
import nl.moj.worker.java.JavaService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperationListener {

    private final JmsTemplate jmsTemplate;
    private final JavaService javaService;

    @JmsListener(destination = "compile_request")
    public void receiveCompileRequest(JMSCompileRequest compileRequest) {
        log.info("Received compile attempt request {}", compileRequest.getAttempt());
        try {
            javaService.compile(compileRequest).thenApply(cr -> {
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
                        .build());
            } catch (Throwable t) {
                log.error("FAIL", t);
            }

        }
    }
}
