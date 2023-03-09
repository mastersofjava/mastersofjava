package nl.moj.server.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nimbusds.jose.shaded.json.JSONObject;
import lombok.RequiredArgsConstructor;
import nl.moj.server.authorization.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@EnableGlobalMethodSecurity(jsr250Enabled = true)
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfiguration {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}");
        return successHandler;
    }

    @Bean //(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(a -> a
                        .antMatchers("/","/error", "/public/**", "/manifest.json", "/browserconfig.xml", "/favicon.ico")
                        .permitAll()
                        .antMatchers("/play", "/feedback", "/rankings")
                        .hasAnyAuthority(Role.USER, Role.GAME_MASTER, Role.ADMIN) // always access
                        .antMatchers("/control", "/bootstrap", "/assignmentAdmin")
                        .hasAnyAuthority(Role.GAME_MASTER, Role.ADMIN) // only facilitators
                        .anyRequest()
                        .authenticated())
                .headers(h -> h.frameOptions().disable())
                .csrf(AbstractHttpConfigurer::disable)
                .logout( l -> l.logoutSuccessHandler(oidcLogoutSuccessHandler()))
                .oauth2Login();
        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper keycloakOidcAuthoritiesMapper() {
        SimpleAuthorityMapper sam = new SimpleAuthorityMapper();
        sam.setPrefix("ROLE_");
        sam.setConvertToUpperCase(true);

        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    OidcIdToken idToken = oidcUserAuthority.getIdToken();
                    // Map the claims found in idToken and/or userInfo
                    // to one or more GrantedAuthority's and add it to mappedAuthorities
                    Map<String, Collection<String>> realmAccess = idToken.getClaim("realm_access");
                    if (realmAccess != null) {
                        Collection<String> roles = realmAccess.get("roles");
                        mappedAuthorities.addAll(roles.stream()
                                .map(SimpleGrantedAuthority::new).toList());
                    }
                    String clientId = idToken.getClaim("azp");
                    Map<String, Collection<Object>> resourceAccess = idToken.getClaim("resource_access");
                    if (resourceAccess != null) {
                        JSONObject client = (JSONObject) resourceAccess.get(clientId);
                        Collection<String> roles = (Collection<String>) client.get("roles");
                        mappedAuthorities.addAll(roles.stream().map(SimpleGrantedAuthority::new).toList());
                    }
                }
            });
            return sam.mapAuthorities(mappedAuthorities);
        };
    }
}
