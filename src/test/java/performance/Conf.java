package performance;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static performance.PerformanceTest.random;

public class Conf {

    // number of teams
    public static int teams = 75; // 100; (We streven nu naar 100 teams)
    // time in seconds that users starting up (reading the assignment etc)
    public static long ramp = 30; // 200; (Binnen 2 minuten heeft de helft van de teams voor het eerst getest, enkelen doen er veel langer over)
    // Every user will start with a compile, and then run 'x' attempts before submitting
    public static int attemptCount = 12; // 8; (een gemiddeld team doet 12,5 test pogingen, maar slechts 56% daarvan komt door de compile heen)
    public static Supplier<Integer> waitTimeBetweenSubmits = () -> random(0, 60); // 8 submits in 24 minuten = om de 3 minuten gemiddeld een submit.

    public final static String mojServerUrl = System.getProperty("moj.server.url","http://localhost:8080");

    public final static String keyCloakUrl = System.getProperty("moj.iam.url","http://localhost:8888");

    public static final String keyCloakAdminUsername = System.getProperty("moj.iam.username","keycloak");

    public static final String keyCloakAdminPassword = System.getProperty("moj.iam.password","i-dont-think-so");

    public static final String prefix = System.getProperty("moj.test.prefix", "");
    public static final String mojAmin = "pt-performance";

    public static final TestAssignment assignment = new RequirementHell();
}
