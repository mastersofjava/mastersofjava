package nl.moj.worker.compile.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.JMSCompileRequest;
import nl.moj.common.messages.JMSCompileResponse;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompileListener {

    private final JmsTemplate jmsTemplate;
    private final CompileRunnerService compileService;

    @JmsListener(destination = "compile_request")
    public void receiveCompileRequest(JMSCompileRequest compileRequest) {
        log.info("Received compile attempt request {}", compileRequest.getAttempt());
        try {
            compileService.compile(compileRequest).thenApply(cr -> {
                log.info("Compile attempt request {} finished with {}", cr.getCompileAttemptUuid(), cr);
                try {
                    sendResults(cr);
                } catch( Throwable t ) {
                    log.error("FAIL",t);
                }
                return cr;
            });
        } catch (Exception e) {
            log.error("Compile failed for attempt {}", compileRequest.getAttempt(), e);
            try {
                fail(compileRequest, e);
            } catch( Throwable t ) {
                log.error("FAIL",t);
            }

        }
    }

    private void sendResults(CompileResult cr) {
        jmsTemplate.convertAndSend("compile_response", JMSCompileResponse.builder()
                .attempt(cr.getCompileAttemptUuid())
                .aborted(cr.isAborted())
                .timeout(cr.isTimeout())
                .success(cr.isSuccess())
                .reason(cr.getReason())
                .started(cr.getDateTimeStart())
                .ended(cr.getDateTimeEnd())
                .output(cr.getCompileOutput())
                .build());
    }

    private void fail(JMSCompileRequest compileRequest, Throwable t) {
        jmsTemplate.convertAndSend("compile_response", JMSCompileResponse.builder()
                .attempt(compileRequest.getAttempt())
                .ended(Instant.now())
                .started(Instant.now())
                .aborted(true)
                .reason(t.getMessage())
                .success(false)
                .timeout(false)
                .build());
    }
}
