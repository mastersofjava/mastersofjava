package nl.moj.server.support;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import nl.moj.common.messages.JMSCompileResponse;
import nl.moj.common.messages.JMSSubmitResponse;
import nl.moj.common.messages.JMSTestResponse;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.message.service.JmsMessageListener;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.test.service.TestService;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@Primary
public class TestJmsListener extends JmsMessageListener {

    private final Map<UUID, CountDownLatch> latches = new ConcurrentHashMap<>();

    public TestJmsListener(SubmitService submitService, CompileService compileService, TestService testService) {
        super(submitService, compileService, testService);
    }

    public void reset() {
        latches.clear();
    }

    @Override
    @JmsListener(destination = "compile_response")
    public void receiveCompileResponse(JMSCompileResponse compileResponse) {
        try {
            super.receiveCompileResponse(compileResponse);
        } finally {
            countDown(compileResponse.getAttempt());
        }

    }

    @Override
    @JmsListener(destination = "test_response")
    public void receiveTestResponse(JMSTestResponse testResponse) {
        try {
            super.receiveTestResponse(testResponse);
        } finally {
            countDown(testResponse.getAttempt());
        }
    }

    @Override
    @JmsListener(destination = "submit_response")
    public void receiveSubmitResponse(JMSSubmitResponse submitResponse) {
        try {
            super.receiveSubmitResponse(submitResponse);
        } finally {
            countDown(submitResponse.getAttempt());
        }
    }

    private void countDown(UUID attempt) {
        if (latches.containsKey(attempt)) {
            latches.get(attempt).countDown();
        }
    }

    public boolean awaitAttempt(UUID attempt, long timeout, TimeUnit unit) throws InterruptedException {
        if (!latches.containsKey(attempt)) {
            latches.put(attempt, new CountDownLatch(1));
        }
        return latches.get(attempt).await(timeout, unit);
    }
}
