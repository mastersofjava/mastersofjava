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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.AllArgsConstructor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;

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
				path = mojServerProperties.getDirectories().getBaseDirectory()
						.resolve(mojServerProperties.getDirectories().getJavadocDirectory());
			}
			registry.addResourceHandler("/javadoc/**").addResourceLocations(path.toAbsolutePath().toUri().toString());
		}
	}

	@KeycloakConfiguration
	@EnableGlobalMethodSecurity(jsr250Enabled = true)
	public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

		// Submits the KeycloakAuthenticationProvider to the AuthenticationManager
		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
			keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
			auth.authenticationProvider(keycloakAuthenticationProvider);
		}

		@Bean
		public KeycloakSpringBootConfigResolver KeycloakConfigResolver() {
			return new KeycloakSpringBootConfigResolver();
		}

		@Bean
		@Override
		protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
			return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
		}
		     
		@Bean
		public PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			super.configure(http);
			http.authorizeRequests()
					.antMatchers("/", "/feedback", "/rankings").hasAnyAuthority(Role.USER, Role.GAME_MASTER, Role.ADMIN) // always access
					.antMatchers("/control", "/bootstrap","/assignmentAdmin").hasAnyAuthority(Role.GAME_MASTER, Role.ADMIN) // only facilitators
					.anyRequest().permitAll()
                    .and().headers().frameOptions().disable()
                    .and().csrf().disable();
		}
	}
}
