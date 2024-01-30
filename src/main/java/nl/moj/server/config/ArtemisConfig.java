package nl.moj.server.config;

import java.util.Collections;

import org.apache.activemq.artemis.core.config.MetricsConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.modes.Mode;
import nl.moj.server.metrics.MojMetricsPlugin;

@Configuration
@RequiredArgsConstructor
public class ArtemisConfig implements ArtemisConfigurationCustomizer {

    private final MojServerProperties mojServerProperties;

    private final MeterRegistry meterRegistry;

    @Override
    public void customize(org.apache.activemq.artemis.core.config.Configuration configuration) {
        try {
            if (mojServerProperties.getMode() == Mode.CONTROLLER) {
                configuration.addAcceptorConfiguration("remote", "tcp://0.0.0.0:61616");
            }
            MetricsConfiguration mc = new MetricsConfiguration();
            mc.setPlugin(new MojMetricsPlugin(meterRegistry));
            mc.getPlugin().init(Collections.emptyMap());
            configuration.setMetricsConfiguration(mc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
