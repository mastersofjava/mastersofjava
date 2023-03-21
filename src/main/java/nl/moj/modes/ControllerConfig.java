package nl.moj.modes;

import nl.moj.modes.condition.ConditionalOnMode;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMode(mode=Mode.CONTROLLER)
@ComponentScan(basePackages = "nl.moj.server")
public class ControllerConfig {
}
