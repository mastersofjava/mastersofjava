package nl.moj.server.user.service;

import javax.transaction.Transactional;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.user.model.User;
import nl.moj.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final TeamRepository teamRepository;

    // TODO this should probably be persisted somewhere in the future.
    private static final Set<User> ACTIVE_USERS = new CopyOnWriteArraySet<>();

    @Transactional
    public User createOrUpdate(Principal principal) {
        if (principal instanceof Authentication a) {
            Map<String, Object> attributes = new HashMap<>();
            if (a.getPrincipal() instanceof ClaimAccessor ca) {
                attributes = ca.getClaims();
            } else if (a.getPrincipal() instanceof OAuth2AuthenticatedPrincipal ap) {
                attributes = ap.getAttributes();
            }

            if (!attributes.isEmpty()) {
                String name = principal.getName();
                User user = userRepository.findByName(name);
                if (user == null) {
                    user = User.builder()
                            .name(name)
                            .build();
                    LOG.info("Created new user {}", name);
                }

                user.setGivenName((String) attributes.get("given_name"));
                user.setFamilyName((String) attributes.get("family_name"));
                user.setEmail((String) attributes.get("email"));
                return userRepository.save(user);
            }
        }
        throw new IllegalArgumentException("Principal not an Authentication, unable to create/update the user.");
    }

    public User findUser(Principal principal) {
        if (principal.getName() != null) {
            return userRepository.findByName(principal.getName());
        }
        throw new IllegalArgumentException("Principal not a OAuth2AuthenticationToken, unable to find the user.");
    }

    @Transactional
    public User addUserToTeam(User user, Team team) {
        User u = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Could not find user " + user.getId()));
        Team t = teamRepository.findById(team.getId())
                .orElseThrow(() -> new IllegalArgumentException("Could not find team " + team.getId()));
        u.setTeam(t);
        u = userRepository.save(u);
        t.getUsers().add(u);
        return u;
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
        if (evt.getUser() != null) {
            User user = findUser(evt.getUser());
            if (user == null) {
                user = createOrUpdate(evt.getUser());
            }
            log.info("User {} connected.", user.getName());
            ACTIVE_USERS.add(user);
        }
    }

    private void userDisconnected(SessionDisconnectEvent evt) {
        if (evt.getUser() != null) {
            User user = findUser(evt.getUser());
            if (user == null) {
                user = createOrUpdate(evt.getUser());
            }
            log.info("User {} disconnected.", user.getName());
            ACTIVE_USERS.remove(user);
        }
    }

    @Transactional
    public boolean deleteUser(String name) {
        User user = userRepository.findByName(name);
        if (user == null) {
            return false;
        }

        userRepository.deleteByName(name);
        return true;
    }
}
