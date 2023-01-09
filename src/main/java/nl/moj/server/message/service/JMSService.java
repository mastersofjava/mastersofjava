package nl.moj.server.message.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.message.model.operations.CompileOperation;
import nl.moj.server.message.model.operations.TestOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JMSService {

    @Autowired
    private JmsTemplate jmsTemplate;

    @JmsListener(destination = "compile_request")
    public void receiveMessage(CompileOperation compileOperation) {
        log.info("Received compile operation request {} with message: {}", compileOperation.getUuid(), compileOperation.getMessage());
    }

    public void sendCompileOperation(CompileOperation compileOperation) {
        jmsTemplate.convertAndSend("compile_request", compileOperation );
    }

    @JmsListener(destination = "test_request")
    public void receiveMessage(TestOperation compileOperation) {
        log.info("Received test operation request {} with message: {}", compileOperation.getUuid(), compileOperation.getMessage());
    }

    public void sendTestOperation(TestOperation testOperation) {
        jmsTemplate.convertAndSend("test_request", testOperation );
    }
}
