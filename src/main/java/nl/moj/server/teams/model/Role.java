package nl.moj.server.teams.model;

public enum Role {
    ADMIN("ROLE_ADMIN"),
    GAME_MASTER("ROLE_GAME_MASTER"),
    USER("ROLE_USER"),
    ANONYMOUS("ROLE_ANONYMOUS");

    private String authority;

    private Role(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return this.authority;
    }

    public String role() {
        return name();
    }
}
