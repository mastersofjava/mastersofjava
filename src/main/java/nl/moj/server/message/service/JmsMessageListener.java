package nl.moj.server.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.*;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.test.service.TestService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JmsMessageListener {

    private final SubmitService submitService;

    private final CompileService compileService;

    private final TestService testService;

    @JmsListener(destination = "compile_response")
    public void receiveCompileResponse(JMSCompileResponse compileResponse) {
        compileService.receiveCompileResponse(compileResponse);
    }

    @JmsListener(destination = "test_response")
    public void receiveTestResponse(JMSTestResponse testResponse) {
        testService.receiveTestResponse(testResponse);
    }

    @JmsListener(destination = "submit_response")
    public void receiveSubmitResponse(JMSSubmitResponse submitResponse) {
        submitService.receiveSubmitResponse(submitResponse);
    }
}
