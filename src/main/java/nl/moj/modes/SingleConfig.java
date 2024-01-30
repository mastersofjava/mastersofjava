package nl.moj.modes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import nl.moj.modes.condition.ConditionalOnMode;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.worker.controller.ControllerClient;
import nl.moj.worker.controller.LocalControllerClient;

@Configuration
@ConditionalOnMode(mode = Mode.SINGLE, matchIfMissing = true)
@ComponentScan(basePackages = { "nl.moj.server", "nl.moj.worker" })
public class SingleConfig {

    @Bean
    public ControllerClient controllerClient(AssignmentService assignmentService) {
        return new LocalControllerClient(assignmentService);
    }
}
