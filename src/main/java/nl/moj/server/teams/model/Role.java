package nl.moj.server.teams.model;

public enum Role {

    ROLE_CONTROL("ROLE_CONTROL"),
    ROLE_USER("ROLE_USER");

    private String description;

    Role(String description) {
        this.description = description;
    }

    public String toString() {
        return description;
    }
}
