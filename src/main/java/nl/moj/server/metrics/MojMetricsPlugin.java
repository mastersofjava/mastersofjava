package nl.moj.server.metrics;

import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.activemq.artemis.core.server.metrics.ActiveMQMetricsPlugin;

public class MojMetricsPlugin implements ActiveMQMetricsPlugin {

    private final MeterRegistry meterRegistry;

    public MojMetricsPlugin(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ActiveMQMetricsPlugin init(Map<String, String> options) {
        return this;
    }

    @Override
    public MeterRegistry getRegistry() {
        return meterRegistry;
    }

}
