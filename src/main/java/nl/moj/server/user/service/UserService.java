package nl.moj.server.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.teams.model.Team;
import nl.moj.server.user.model.User;
import nl.moj.server.user.repository.UserRepository;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    // TODO this should probably be persisted somewhere in the future.
    private static final Set<User> ACTIVE_USERS = new CopyOnWriteArraySet<>();

    public User createOrUpdate(Principal principal) {
        if (principal instanceof KeycloakAuthenticationToken) {
            KeycloakAuthenticationToken kat = (KeycloakAuthenticationToken) principal;
            AccessToken token = kat.getAccount().getKeycloakSecurityContext().getToken();

            UUID uuid = UUID.fromString(token.getSubject());
            User user = userRepository.findByUuid(uuid);
            if (user == null) {
                user = User.builder()
                        .uuid(uuid)
                        .build();
                LOG.info("Created new user {}", uuid);
            }

            user.setName(token.getName());
            user.setGivenName(token.getGivenName());
            user.setFamilyName(token.getFamilyName());
            user.setEmail(token.getEmail());

            return userRepository.save(user);
        }
        throw new IllegalArgumentException("Principal not a KeycloakAuthenticationToken, unable to create/update the user.");
    }

    public User findUser(Principal principal) {
        if (principal instanceof KeycloakAuthenticationToken) {
            KeycloakAuthenticationToken kat = (KeycloakAuthenticationToken) principal;
            AccessToken token = kat.getAccount().getKeycloakSecurityContext().getToken();
            UUID uuid = UUID.fromString(token.getSubject());

            User u = userRepository.findByUuid(uuid);
            log.info("Found user {}", u);

            //return userRepository.findByUuid(uuid);
            return u;
        }
        throw new IllegalArgumentException("Principal not a KeycloakAuthenticationToken, unable to find the user.");
    }

    public User addUserToTeam(User user, Team team) {
        user.setTeam(team);
        User r = userRepository.save(user);
        team.getUsers().add(r);
        return r;
    }

    public Set<User> getActiveUsers() {
        // TODO check if we cannot use the SessionRegistry for this.
        return Collections.unmodifiableSet(ACTIVE_USERS);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof SessionDisconnectEvent) {
            userDisconnected((SessionDisconnectEvent) applicationEvent);
        }
        if (applicationEvent instanceof SessionConnectedEvent) {
            userConnected((SessionConnectedEvent) applicationEvent);
        }
    }

    private void userConnected(SessionConnectedEvent evt) {
        User user = findUser(evt.getUser());
        log.info("User {} connected.", user.getUuid());
        ACTIVE_USERS.add(user);
    }

    private void userDisconnected(SessionDisconnectEvent evt) {
        User user = findUser(evt.getUser());
        log.info("User {} disconnected.", user.getUuid());
        ACTIVE_USERS.remove(user);
    }
}
