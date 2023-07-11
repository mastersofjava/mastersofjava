package nl.moj.server.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.JMSCompileResponse;
import nl.moj.common.messages.JMSResponse;
import nl.moj.common.messages.JMSSubmitResponse;
import nl.moj.common.messages.JMSTestResponse;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.test.service.TestService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JmsMessageListener {

    protected static final String RESPONSE_DESTINATION = "operation_response";

    private final SubmitService submitService;

    private final CompileService compileService;

    private final TestService testService;

    @JmsListener(destination = RESPONSE_DESTINATION)
    public void receiveOperationResponse(JMSResponse response) {
        if (response instanceof JMSCompileResponse r) {
            receiveCompileResponse(r);
        } else if (response instanceof JMSTestResponse r) {
            receiveTestResponse(r);
        } else if (response instanceof JMSSubmitResponse r) {
            receiveSubmitResponse(r);
        } else {
            log.warn("Unable to receive operation response for type {}, ignoring.", response.getClass().getName());
        }
    }

    private void receiveCompileResponse(JMSCompileResponse compileResponse) {
        compileService.receiveCompileResponse(compileResponse);
    }

    private void receiveTestResponse(JMSTestResponse testResponse) {
        testService.receiveTestResponse(testResponse);
    }

    private void receiveSubmitResponse(JMSSubmitResponse submitResponse) {
        submitService.receiveSubmitResponse(submitResponse);
    }
}
