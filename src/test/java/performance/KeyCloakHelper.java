package performance;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Map;

public class KeyCloakHelper {

    public static String createUserAndReturnToken(String keycloakUrl, String userName) {
        System.out.println("++++++++++++ Creating user " + userName);
        createUser(userName, keycloakUrl);
        System.out.println("------------ User created");
        String token = getToken(keycloakUrl, userName);
        System.out.println("_____________ User creation successful. Token: " + token);
        return token;
    }

    private static void createUser(String userName, String keycloakUrl) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm("master")
                .clientId("admin-cli")
                .username(Conf.keyCloakAdminUsername)
                .password(Conf.keyCloakAdminPassword)
                .build()) {
            RealmResource moj = keycloak.realm("moj");

            UserRepresentation user = new UserRepresentation();
            user.setFirstName(userName);
            user.setLastName(userName);
            user.setEmail(userName + "@mail.com");
            user.setUsername(userName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            moj.users().create(user).close();

            String id = moj.users().search(userName).get(0).getId();
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setTemporary(false);
            credentialRepresentation.setValue(userName);
            moj.users().get(id).resetPassword(credentialRepresentation);
        }
    }

    private static String getToken(String keycloakUrl, String userName) {
        Configuration configuration = new Configuration();
        configuration.setRealm("moj");
        configuration.setAuthServerUrl(keycloakUrl);
        configuration.setResource("gatling");
        configuration.setCredentials(Map.of("secret", Conf.keyCloakClientSecret));
        AuthzClient authzClient = AuthzClient.create(configuration);

        return authzClient.obtainAccessToken(userName + "@mail.com", userName).getToken();
    }


}
