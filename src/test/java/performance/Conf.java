package performance;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static performance.PerformanceTest.random;

public class Conf {

    // number of teams
    public static int teams = 25; // 100; (We streven nu naar 100 teams)
    // time in seconds that users starting up (reading the assignment etc)
    public static long ramp = 30; // 200; (Binnen 2 minuten heeft de helft van de teams voor het eerst getest, enkelen doen er veel langer over)
    // Every user will start with a compile, and then run 'x' attempts before submitting
    public static int attemptCount = 1; // 8; (een gemiddeld team doet 12,5 test pogingen, maar slechts 56% daarvan komt door de compile heen)
    public static Supplier<Integer> waitTimeBetweenSubmits = () -> random(0, 60); // 8 submits in 24 minuten = om de 3 minuten gemiddeld een submit.

    public final static String mojServerUrl = "https://moj.cloud.bliep.net";
    //public final static String mojServerUrl = "http://localhost:8080";
    //public final static String mojServerUrl = "http://mastersofjava.nljug";

    public final static String keyCloakUrl = "https://auth-moj.cloud.bliep.net";
    //public final static String keyCloakUrl = "http://localhost:8888";
    //public final static String keyCloakUrl = "http://auth.mastersofjava.nljug";
    // The secret of the keyCloak client named 'gatling' with 'Client authentication' set to true
    public static final String keyCloakAdminUsername = "keycloak";

    public static final String keyCloakAdminPassword = "CnmUea3C8pWGq4H7NagBikD8Ltf7o9pJgMkLxRU7XDKjPcPmCep4HMkVRBhZniD6";
    // public static final String keyCloakAdminPassword = "keycloak";

    public static final String mojAmin = "performance";

    public static final TestAssignment assignment = new RequirementHell();
}
