package nl.moj.server.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import nl.moj.common.messages.*;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.test.model.TestAttempt;

@Service
public class JMSResponseHelper {

    private static final String ABORT_WORKER = "<abort-worker>";

    @Transactional(Transactional.TxType.MANDATORY)
    public JMSCompileResponse abortResponse(CompileAttempt ca) {
        return JMSCompileResponse.builder()
                .attempt(ca.getUuid())
                .worker(ABORT_WORKER)
                .timeout(false)
                .success(false)
                .aborted(true)
                .started(ca.getDateTimeRegister())
                .ended(Instant.now())
                .output("Compiling timed out.")
                .reason("No response received.")
                .build();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public JMSTestResponse abortResponse(TestAttempt ta) {
        List<JMSTestCaseResult> tcs = new ArrayList<>();
        ta.getTestCases().forEach(tc -> {
            tcs.add(JMSTestCaseResult.builder()
                    .testCase(tc.getUuid())
                    .worker(ABORT_WORKER)
                    .aborted(true)
                    .success(false)
                    .timeout(false)
                    .started(tc.getDateTimeRegister())
                    .ended(Instant.now())
                    .output("Test timed out.")
                    .reason("No response received.")
                    .build());
        });

        return JMSTestResponse.builder()
                .attempt(ta.getUuid())
                .worker(ABORT_WORKER)
                .aborted(true)
                .started(ta.getDateTimeRegister())
                .ended(Instant.now())
                .reason("Testing timed out.")
                .testCaseResults(tcs)
                .compileResponse(abortResponse(ta.getCompileAttempt()))
                .build();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public JMSSubmitResponse abortResponse(SubmitAttempt sa) {
        return JMSSubmitResponse.builder()
                .attempt(sa.getUuid())
                .worker(ABORT_WORKER)
                .aborted(true)
                .started(sa.getDateTimeRegister())
                .ended(Instant.now())
                .reason("No response received.")
                .testResponse(abortResponse(sa.getTestAttempt()))
                .build();
    }
}
