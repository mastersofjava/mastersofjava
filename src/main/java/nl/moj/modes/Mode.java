package nl.moj.modes;

import java.util.Arrays;

public enum Mode {
    SINGLE,
    WORKER,
    CONTROLLER;

    public static final Mode DEFAULT = SINGLE;

    public boolean anyMatch(Mode... modes) {
        return Arrays.stream(modes).anyMatch(m -> this == m);
    }
}
