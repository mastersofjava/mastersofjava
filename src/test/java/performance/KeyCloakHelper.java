package performance;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class KeyCloakHelper {

    @Test
    public void test() {
        String userName = "TestUser10:35:54:324";
        System.out.println("   TOKEN: " + createUserAndReturnToken(userName));
    }

    public static String createUserAndReturnToken(String userName) {
        System.out.println("++++++++++++ Creating user " + userName);
        createUser(userName);
//        System.out.println("------------ User created");
        String token = getToken(userName);
//        System.out.println("_____________ User creation successful. Token: " + token);
        return token;
    }

    private static void createUser(String userName) {
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

            moj.users().create(user).close();

            String id = moj.users().search(userName).get(0).getId();
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setTemporary(false);
            credentialRepresentation.setValue(userName);
            moj.users().get(id).resetPassword(credentialRepresentation);
        }
    }


    private static String getToken(String userName) {
        try(var httpclient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(Conf.keyCloakUrl + "/realms/moj/protocol/openid-connect/token");

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("client_id", "moj"));
            params.add(new BasicNameValuePair("username", userName + "@mail.com"));
            params.add(new BasicNameValuePair("password", userName));
            params.add(new BasicNameValuePair("grant_type", "password"));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpResponse response = httpclient.execute(httppost);
            InputStream responseJson = response.getEntity().getContent();

            Scanner scanner = new Scanner(responseJson).useDelimiter("\\A");
            String responseBody = scanner.hasNext() ? scanner.next() : "";
            JSONObject jsonObject = new JSONObject(responseBody);
            return jsonObject.getString("access_token");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
