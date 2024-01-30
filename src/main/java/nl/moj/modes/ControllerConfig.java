package nl.moj.modes;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import nl.moj.modes.condition.ConditionalOnMode;

@Configuration
@ConditionalOnMode(mode = Mode.CONTROLLER)
@ComponentScan(basePackages = "nl.moj.server")
public class ControllerConfig {
}
