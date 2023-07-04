package performance.util;

import java.time.Instant;
import java.util.Objects;

public class OidcToken {

    private String accessToken;
    private String refreshToken;
    private Instant expires;

    public OidcToken(String accessToken, String refreshToken, Instant expires) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expires = expires;
    }

    public String bearerHeader() {
        return String.format("Bearer %s", getAccessToken());
    }

    public boolean needsRefresh() {
        return expires.minusSeconds(30).isBefore(Instant.now());
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Instant getExpires() {
        return expires;
    }

    public String getAccessToken() {
        if (needsRefresh()) {
            OidcToken token = RestClient.refresh(this);
            this.accessToken = token.getAccessToken();
            this.refreshToken = token.getRefreshToken();
            this.expires = token.getExpires();
        }
        return this.accessToken;
    }

    @Override
    public String toString() {
        return "OidcToken{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", expires=" + expires +
                '}';
    }
}
