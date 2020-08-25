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
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

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
            return userRepository.findByUuid(uuid);
        }
        throw new IllegalArgumentException("Principal not a KeycloakAuthenticationToken, unable to create/update the user.");
    }

    public User addUserToTeam(User user, Team team) {
        user.setTeam(team);
        return userRepository.save(user);
    }
}
