package nl.moj.server.support;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import nl.moj.common.messages.JMSResponse;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.message.service.JmsMessageListener;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.test.service.TestService;

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
    @JmsListener(destination = RESPONSE_DESTINATION)
    public void receiveOperationResponse(JMSResponse response) {
        try {
            super.receiveOperationResponse(response);
        } finally {
            countDown(response.getAttempt());
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
