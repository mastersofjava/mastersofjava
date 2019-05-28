package nl.moj.server.config;

import lombok.AllArgsConstructor;
import nl.moj.server.TeamDetailsService;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@AllArgsConstructor
public class WebConfiguration {

    private MojServerProperties mojServerProperties;

    @Configuration
    public class WebConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            Path path = Paths.get(mojServerProperties.getDirectories().getJavadocDirectory());
            if (!path.isAbsolute()) {
                path = mojServerProperties.getDirectories().getBaseDirectory().resolve(
                        mojServerProperties.getDirectories().getJavadocDirectory());
                System.out.println("javadoc -> " + path.toAbsolutePath().toUri().toString());
            }
            registry
                    .addResourceHandler("/javadoc/**")
                    .addResourceLocations(path.toAbsolutePath().toUri().toString());
        }
    }

    @EnableWebSecurity
    @EnableGlobalMethodSecurity(jsr250Enabled = true)
    @Configuration
    public class SecurityConfig extends WebSecurityConfigurerAdapter {

        private TeamDetailsService teamDetailsService;

        public SecurityConfig(TeamDetailsService teamDetailsService) {
            this.teamDetailsService = teamDetailsService;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        private DaoAuthenticationProvider authProvider() {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(teamDetailsService);
            authProvider.setPasswordEncoder(passwordEncoder());
            return authProvider;
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(authProvider());
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .antMatchers("/login", "/register", "/feedback", "/bootstrap").permitAll()//
                    .antMatchers("/").hasRole(Role.USER.name()) //
                    .antMatchers("/control").hasAnyRole(Role.GAME_MASTER.name(), Role.ADMIN.name()) //

                    .and().formLogin().successHandler(new CustomAuthenticationSuccessHandler()).loginPage("/login")
                    .and().logout().and().headers().frameOptions().disable().and().csrf().disable();
        }

        private class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                response.setStatus(HttpServletResponse.SC_OK);
                List<String> roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

                if( roles.contains(Role.ADMIN.authority()) || roles.contains(Role.GAME_MASTER.authority())) {
                    response.sendRedirect("/control");
                } else if( roles.contains(Role.USER.authority())){
                    teamDetailsService.initTeam(authentication.getName());
                    response.sendRedirect("/");
                } else {
                    response.sendRedirect("/");
                }
            }
        }
    }
}
