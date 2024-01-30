package nl.moj.modes;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import nl.moj.common.assignment.service.AssignmentDescriptorService;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.modes.condition.ConditionalOnMode;
import nl.moj.worker.controller.ControllerClient;
import nl.moj.worker.controller.RemoteControllerClient;

@Configuration
@ConditionalOnMode(mode = Mode.WORKER)
@ComponentScan(basePackages = "nl.moj.worker")
public class WorkerConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public ControllerClient controllerClient(MojServerProperties mojServerProperties, RestTemplate restTemplate,
            AssignmentDescriptorService ads) {
        return new RemoteControllerClient(mojServerProperties, ads, restTemplate);
    }
}
