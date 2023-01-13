package nl.moj.worker;

import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.MDC;

public class RunTracer {

    public static <T> T trace(UUID runId, Supplier<T> block) {
        try(MDC.MDCCloseable ignored = MDC.putCloseable("run", runId.toString())) {
            return block.get();
        }
    }

    public static void trace(UUID runId, Runnable block) {
        try(MDC.MDCCloseable ignored = MDC.putCloseable("run", runId.toString())) {
            block.run();
        }
    }
}
