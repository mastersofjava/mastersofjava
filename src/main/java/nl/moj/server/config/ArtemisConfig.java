package nl.moj.server.config;

import nl.moj.modes.Mode;
import nl.moj.modes.condition.ConditionalOnMode;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMode(mode= Mode.CONTROLLER)
public class ArtemisConfig implements ArtemisConfigurationCustomizer {

    @Override
    public void customize(org.apache.activemq.artemis.core.config.Configuration configuration) {
        try {
            configuration.addAcceptorConfiguration("remote", "tcp://0.0.0.0:61616");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
