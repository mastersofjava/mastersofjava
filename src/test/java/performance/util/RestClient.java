package performance.util;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import performance.Conf;

public class RestClient {

    private static final Logger log = LoggerFactory.getLogger(RestClient.class);

    public static User createUser(String userName) {
        return createUserInGroups(userName, RandomStringUtils.random(16, true, true), "user");
    }

    public static User createAdminUser(String userName) {
        return createUserInGroups(userName, RandomStringUtils.random(16, true, true), "admin");
    }

    private static User createUserInGroups(String userName, String password, String... groups) {
        log.info("Using keycloak " + Conf.keyCloakUrl);
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(Conf.keyCloakUrl)
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

            if (groups.length > 0) {
                user.setGroups(Arrays.asList(groups));
            }

            moj.users().create(user).close();

            String id = moj.users().search(userName).get(0).getId();
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setTemporary(false);
            credentialRepresentation.setValue(password);
            moj.users().get(id).resetPassword(credentialRepresentation);

            return new User(id, userName, password, null );
        }
    }

    public static void deleteKeycloakUser(User user) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(Conf.keyCloakUrl)
                .realm("master")
                .clientId("admin-cli")
                .username(Conf.keyCloakAdminUsername)
                .password(Conf.keyCloakAdminPassword)
                .build()) {
            RealmResource moj = keycloak.realm("moj");

            try (Response ignored = moj.users().delete(user.id())) {
                if (ignored.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    throw new RuntimeException(String.format("Failed to delete keycloak user %s", user.username()));
                }
            }
        }
    }

    public static void deleteUser(User auth, User user) {
        OidcToken token = getOidcToken(auth);
        try (var httpclient = HttpClients.createDefault()) {
            HttpDelete delete = new HttpDelete(Conf.mojServerUrl + "/users/" + user.username());
            delete.addHeader("Authorization", token.bearerHeader());

            HttpResponse response = httpclient.execute(delete);
            if (response.getStatusLine().getStatusCode() < 200 && response.getStatusLine().getStatusCode() >= 300) {
                throw new RuntimeException(String.format("Failed to delete user  %s", user.username()));
            }
        } catch (Exception e) {
            log.error("Failed to delete user {}", user.username(), e);
            throw new RuntimeException(e);
        }
    }

    public static void deleteTeam(User auth, String name) {
        OidcToken token = getOidcToken(auth);
        try (var httpclient = HttpClients.createDefault()) {
            HttpDelete delete = new HttpDelete(Conf.mojServerUrl + "/team/" + name);
            delete.addHeader("Authorization", token.bearerHeader());

            HttpResponse response = httpclient.execute(delete);
            if (response.getStatusLine().getStatusCode() < 200 && response.getStatusLine().getStatusCode() >= 300) {
                throw new RuntimeException(String.format("Failed to delete team  %s", name));
            }
        } catch (Exception e) {
            log.error("Unable to delete team {}.", name ,e);
            throw new RuntimeException(e);
        }
    }

    public static OidcToken getOidcToken(User user) {
        try (var httpclient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(Conf.keyCloakUrl + "/realms/moj/protocol/openid-connect/token");

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("client_id", "moj"));
            params.add(new BasicNameValuePair("username", user.username() + "@mail.com"));
            params.add(new BasicNameValuePair("password", user.password()));
            params.add(new BasicNameValuePair("grant_type", "password"));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpResponse response = httpclient.execute(httppost);
            InputStream responseJson = response.getEntity().getContent();

            Scanner scanner = new Scanner(responseJson).useDelimiter("\\A");
            String responseBody = scanner.hasNext() ? scanner.next() : "";
            JSONObject jsonObject = new JSONObject(responseBody);
            return new OidcToken(user.username(),jsonObject.getString("access_token"), jsonObject.getString("refresh_token"), Instant.now().plusSeconds(jsonObject.getInt("expires_in")));
        } catch (Exception e) {
            log.error("Unable to get access token.",e);
            throw new RuntimeException(e);
        }
    }

    public static OidcToken refresh(OidcToken token) {
        log.info("Refreshing token for user {}.", token.getUser());
        if( token.needsRefresh()) {
            try (var httpclient = HttpClients.createDefault()) {
                HttpPost httppost = new HttpPost(Conf.keyCloakUrl + "/realms/moj/protocol/openid-connect/token");

                List<NameValuePair> params = new ArrayList<NameValuePair>(2);
                params.add(new BasicNameValuePair("client_id", "moj"));
                params.add(new BasicNameValuePair("refresh_token", token.getRefreshToken()));
                params.add(new BasicNameValuePair("grant_type", "refresh_token"));
                httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

                HttpResponse response = httpclient.execute(httppost);
                InputStream responseJson = response.getEntity().getContent();

                Scanner scanner = new Scanner(responseJson).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                JSONObject jsonObject = new JSONObject(responseBody);

                return new OidcToken(token.getUser(), jsonObject.getString("access_token"), jsonObject.getString("refresh_token"), Instant.now().plusSeconds(jsonObject.getInt("expires_in")));
            } catch (Exception e) {
                log.error("Unable to refresh access token.",e);
                throw new RuntimeException(e);
            }
        }
        return token;
    }
}
