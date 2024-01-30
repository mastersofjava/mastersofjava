package nl.moj.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import nl.moj.modes.Mode;
import nl.moj.modes.condition.ConditionalOnMode;

@Configuration
@ConditionalOnMode(mode = Mode.WORKER)
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean //(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(a -> a
                .antMatchers("/actuator/health")
                .permitAll()
                .anyRequest()
                .denyAll());
        return http.build();
    }
}
