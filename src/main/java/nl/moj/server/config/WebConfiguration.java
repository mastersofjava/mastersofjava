/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
                    .antMatchers("/").hasAuthority(Role.USER) //
                    .antMatchers("/control").hasAnyAuthority(Role.GAME_MASTER, Role.ADMIN) //

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

                if (roles.contains(Role.ADMIN) || roles.contains(Role.GAME_MASTER)) {
                    response.sendRedirect("/control");
                } else if (roles.contains(Role.USER)) {
                    teamDetailsService.initTeam(authentication.getName());
                    response.sendRedirect("/");
                } else {
                    response.sendRedirect("/");
                }
            }
        }
    }
}
