package nl.moj.server.user.service;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.teams.model.Team;
import nl.moj.server.user.model.User;
import nl.moj.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.transaction.Transactional;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    // TODO this should probably be persisted somewhere in the future.
    private static final Set<User> ACTIVE_USERS = new CopyOnWriteArraySet<>();

    @Transactional
    public User createOrUpdate(Principal principal) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication != null) {
//            if (authentication instanceof OAuth2User user) {
//                String username = user.getName();
//                try {
//                    return (UserDto) userDetailsService.loadUserByUsername(username);
//                } catch (Exception e) {
//                    return null;
//                }
//            } else if( authentication instanceof OAuth2AuthenticationToken token ) {
//                String username = token.getName();
//                try {
//                    return (UserDto) userDetailsService.loadUserByUsername(username);
//                } catch (Exception e) {
//                    return null;
//                }
//            }
//        }

        if (principal.getName() != null ) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) principal;
            User user = userRepository.findByName(principal.getName());
            if (user == null) {
                user = User.builder()
                        .name(principal.getName())
                        .build();
                LOG.info("Created new user {}", principal.getName());
            }

            user.setGivenName(token.getPrincipal().getAttribute("given_name"));
            user.setFamilyName(token.getPrincipal().getAttribute("family_name"));
            user.setEmail(token.getPrincipal().getAttribute("email"));

            return userRepository.save(user);
        }
        throw new IllegalArgumentException("Principal not a OAuth2AuthenticationToken, unable to create/update the user.");
    }

    public User findUser(Principal principal) {
        if( principal.getName() != null ) {
            return userRepository.findByName(principal.getName());
        }
        throw new IllegalArgumentException("Principal not a OAuth2AuthenticationToken, unable to find the user.");
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
        if( evt.getUser() != null ) {
            User user = findUser(evt.getUser());
            if( user == null ) {
                user = createOrUpdate(evt.getUser());
            }
            log.info("User {} connected.", user.getName());
            ACTIVE_USERS.add(user);
        }
    }

    private void userDisconnected(SessionDisconnectEvent evt) {
        if( evt.getUser() != null ) {
            User user = findUser(evt.getUser());
            if( user == null ) {
                user = createOrUpdate(evt.getUser());
            }
            log.info("User {} disconnected.", user.getName());
            ACTIVE_USERS.remove(user);
        }
    }
}
