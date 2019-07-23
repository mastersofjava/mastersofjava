package nl.moj.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.MojServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@EnableConfigurationProperties(MojServerProperties.class)
@ServletComponentScan
@RequiredArgsConstructor
@Slf4j
public class MojServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MojServerApplication.class, args);
    }

}