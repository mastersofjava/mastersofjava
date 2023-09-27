package performance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.gatling.core.json.Json;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import performance.util.OidcToken;
import performance.util.RestClient;
import performance.util.User;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PerformanceTest extends Simulation {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);

    private final HttpProtocolBuilder httpProtocol = http.baseUrl(Conf.mojServerUrl)
            .header("Authorization", session -> ((OidcToken) Objects.requireNonNull(session.get("token"))).bearerHeader())
            .userAgentHeader("Gatling2")
            .wsBaseUrl(Conf.mojServerUrl.replace("http", "ws"))
            .wsAutoReplyTextFrame(s -> {
                // auto reply ping STOMP ping messages
                if( s != null && s.length() == 1 ) {
                    return "\n";
                }
                return null;
            })
            .wsMaxReconnects(1000);

    private String superAwesomeNamingServiceUUID;
    private final List<String> testUUIDs = new ArrayList<>();

    private final List<User> users = new ArrayList<>();
    private String sessionUUID;

    private User mojAdmin = null;

    private String runId = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").format(LocalDateTime.now());

    private final ChainBuilder createUser = exec(session -> {
        User user = initializeUser(session);
        users.add(user);
        return session.set("user", user).set("token", RestClient.getOidcToken(user));
    });

    // do we need this?
    private final ChainBuilder joinCompetition = exec(s -> {
        http("Join Competition").get("/play").check(status().is(200));
        return s;
    });

    private final ChainBuilder createTeam = exec(
            http("Create Team").post("/team")
                    .formParam("name", session -> {
                        log.info("Using team name {}", ((User) session.get("user")).team());
                        return ((User) session.get("user")).team();
                    })
                    .formParam("company", "Monkey Inc")
                    .formParam("country", "NL")
                    .formParam("submit", "")
                    .check(status().is(200)));

    private final ChainBuilder joinAssignment = exec(http("Get assignment info").get("/play")
            .check(status().is(200))
            .check(bodyString().saveAs("assignmentHtml"))).exec(session -> {
                parseHtml(session.getString("assignmentHtml"));
                return session;
            })
            .exec(ws("Connect WebSocket")
                    .connect("/ws/session/websocket")
                    .onConnected(exec(ws("Connect with Stomp").sendText("""
                                    CONNECT
                                    accept-version:1.0,1.1,1.2
                                    heart-beat:4000,4000

                                    \u0000
                                    """)
                            .await(10)
                            .on(ws.checkTextMessage("Check connection")
                                    .check(regex("CONNECTED\n.*")))).exec(ws("Subscribe to user destination").sendText("""
                            SUBSCRIBE
                            ack:client
                            id:sub-0
                            destination:/user/queue/session

                            \u0000
                            """))));

    private final ChainBuilder successCompile = exec(
            ws("Compile (Success) Request")
                    .sendText(session -> getCompileMessage(Conf.assignment.getEmpty()))
                    .await(10)
                    .on(ws.checkTextMessage("Compile Started").check(regex(".*COMPILING_STARTED.*")).silent(),
                            ws.checkTextMessage("Compile (Success) Response")
                                    .check(regex(".*COMPILE.*success\":true.*")))
    );

    private final ChainBuilder failCompile = exec(ws("Compile (Failure) Request")
            .sendText(session -> getCompileMessage(Conf.assignment.getDoesNotCompile()))
            .await(10)
            .on(ws.checkTextMessage("Compile Started").check(regex(".*COMPILING_STARTED.*")).silent(),
                    ws.checkTextMessage("Compile (Failure) Response").check(regex(".*COMPILE.*success\":false.*"))));

    private final ChainBuilder attemptCompileAndTest = exec(
            ws("Test Attempt Request")
                    // randomly choose how many UT's will fail.
                    .sendText(session -> getTestMessage(
                            either(createOptions(Conf.assignment.getAttempts()))))
                    .await(30)
                    .on(
                            ws.checkTextMessage("Attempt Testing Started")
                                    .check(bodyString().saveAs("msg_#{i}_0"))
                                    .check(substring("TESTING_STARTED").exists()),
                            ws.checkTextMessage("Attempt Compile Response")
                                    .check(bodyString().saveAs("msg_#{i}_1"))
                                    .check(regex(".*COMPILE.*success\":true.*")),
                            ws.checkTextMessage("Attempt Test0 Result Received")
                                    .check(bodyString().saveAs("msg_#{i}_2"))
                                    .check(regex(".+\"test\":\"Test\\d\",.+")),
//                                    .check(substring(",\"test\":\"Test").exists())
//                                    .check(substring(",\"test\":\"Test0\",").exists()),
                            ws.checkTextMessage("Attempt Test1 Result Received")
                                    .check(bodyString().saveAs("msg_#{i}_3"))
                                    .check(regex(".+\"test\":\"Test\\d\",.+")),
//                                    .check(substring(",\"test\":\"Test").exists())
//                                    .check(substring(",\"test\":\"Test1\",").exists()),
                            ws.checkTextMessage("Attempt Test2 Result Received")
                                    .check(bodyString().saveAs("msg_#{i}_4"))
                                    .check(regex(".+\"test\":\"Test\\d\",.+")),
//                                    .check(substring(",\"test\":\"Test").exists())
//                                    .check(substring(",\"test\":\"Test2\",").exists()),
                            ws.checkTextMessage("Attempt Test3 Result Received")
                                    .check(bodyString().saveAs("msg_#{i}_5"))
                                    .check(regex(".+\"test\":\"Test\\d\",.+")),
//                                    .check(substring(",\"test\":\"Test").exists())
//                                    .check(substring(",\"test\":\"Test3\",").exists()),
                            ws.checkTextMessage("Attempt Test4 Result Received")
                                    .check(bodyString().saveAs("msg_#{i}_6"))
                                    .check(regex(".+\"test\":\"Test\\d\",.+")),
//                                    .check(substring(",\"test\":\"Test").exists())
//                                    .check(substring(",\"test\":\"Test4\",").exists()),
                            ws.checkTextMessage("Attempt Test5 Result Received")
                                    .check(bodyString().saveAs("msg_#{i}_7"))
                                    .check(regex(".+\"test\":\"Test\\d\",.+")),
//                                    .check(substring(",\"test\":\"Test").exists())
//                                    .check(substring(",\"test\":\"Test5\",").exists()),
                            ws.checkTextMessage("Attempt Test6 Result Received")
                                    .check(bodyString().saveAs("msg_#{i}_8"))
                                    .check(regex(".+\"test\":\"Test\\d\",.+"))
//                                    .check(substring(",\"test\":\"Test").exists())
//                                    .check(substring(",\"test\":\"Test6\",").exists())
                    )
    ).exec(session -> {
        appendToFile(session.userId(), "**** BEGIN ATTEMPT " + session.get("i") + " ****\n");
        for (int i = 0; i < 9; i++) {
            if (session.contains("msg_#{i}_" + i)) {
                appendToFile(session.userId(), ">>>>>\n" + session.get("msg_#{i}_" + i) + "\n<<<<<\n");
            } else {
                appendToFile(session.userId(), "Missing response " + i + "\n");
            }
        }
        appendToFile(session.userId(), "**** END ATTEMPT " + session.get("i") + " ****\n");
        return session;
    });

    private final ChainBuilder submit = exec(ws("Submit Request").sendText(session -> getSubmitMessage(Conf.assignment.getSolution()))
            .await(30)
            .on(ws.checkTextMessage("Submit Started")
                            .check(bodyString().saveAs("submit_0"))
                            .check(regex(".*SUBMIT_STARTED.*")),
                    ws.checkTextMessage("Submit Compile Response")
                            .check(bodyString().saveAs("submit_1"))
                            .check(regex(".*COMPILE.*success.*true.*")),
                    ws.checkTextMessage("Submit Hidden Test Result Received")
                            .check(bodyString().saveAs("submit_2"))
                            .check(substring(",\"test\":\"HiddenTest\",").exists()),
                    ws.checkTextMessage("Submit Test0 Result Received")
                            .check(bodyString().saveAs("submit_3"))
                            .check(regex(".+\"test\":\"Test\\d\",.+")),
//                            .check(substring(",\"test\":\"Test").exists())
//                            .check(substring(",\"test\":\"Test0\",").exists()),
                    ws.checkTextMessage("Submit Test1 Result Received")
                            .check(bodyString().saveAs("submit_4"))
                            .check(regex(".+\"test\":\"Test\\d\",.+")),
//                            .check(substring(",\"test\":\"Test").exists())
//                            .check(substring(",\"test\":\"Test1\",").exists()),
                    ws.checkTextMessage("Submit Test2 Result Received")
                            .check(bodyString().saveAs("submit_5"))
                            .check(regex(".+\"test\":\"Test\\d\",.+")),
//                            .check(substring(",\"test\":\"Test").exists())
//                            .check(substring(",\"test\":\"Test2\",").exists()),
                    ws.checkTextMessage("Submit Test3 Result Received")
                            .check(bodyString().saveAs("submit_6"))
                            .check(regex(".+\"test\":\"Test\\d\",.+")),
//                            .check(substring(",\"test\":\"Test").exists())
//                            .check(substring(",\"test\":\"Test3\",").exists()),
                    ws.checkTextMessage("Submit Test4 Result Received")
                            .check(bodyString().saveAs("submit_7"))
                            .check(regex(".+\"test\":\"Test\\d\",.+")),
//                            .check(substring(",\"test\":\"Test").exists())
//                            .check(substring(",\"test\":\"Test4\",").exists()),
                    ws.checkTextMessage("Submit Test5 Result Received")
                            .check(bodyString().saveAs("submit_8"))
                            .check(regex(".+\"test\":\"Test\\d\",.+")),
//                            .check(substring(",\"test\":\"Test").exists())
//                            .check(substring(",\"test\":\"Test5\",").exists()),
                    ws.checkTextMessage("Submit Test6 Result Received")
                            .check(bodyString().saveAs("submit_9"))
                            .check(regex(".+\"test\":\"Test\\d\",.+")),
//                            .check(substring(",\"test\":\"Test").exists())
//                            .check(substring(",\"test\":\"Test6\",").exists()),
                    ws.checkTextMessage("Submit Results Received")
                            .check(bodyString().saveAs("submit_10"))
                            .check(regex(".*SUBMIT.*"))))
            .exec(session -> {
                appendToFile(session.userId(), "**** BEGIN SUBMIT ****\n");
                for (int i = 0; i < 11; i++) {
                    if (session.contains("submit_" + i)) {
                        appendToFile(session.userId(), ">>>>>\n" + session.get("submit_" + i) + "\n<<<<<\n");
                    } else {
                        appendToFile(session.userId(), "Missing response " + i + "\n");
                    }
                }
                appendToFile(session.userId(), "**** END SUBMIT ****\n");
                return session;
            });

    public void before() {
        mojAdmin = RestClient.createAdminUser(Conf.mojAmin);
    }

    public void after() {
        // clean up
        for (User user : users) {
            try {
                RestClient.deleteTeam(mojAdmin, user.team());
                RestClient.deleteKeycloakUser(user);
            } catch (Exception e) {
                log.error("Cleanup of user {} failed", user.username(), e);
            }
        }
        RestClient.deleteKeycloakUser(mojAdmin);
    }

    public PerformanceTest() {
        // This is where the test starts:
        ScenarioBuilder scn = scenario("Test " + Conf.assignment.getAssignmentName())
                .exec(createUser, createTeam, joinAssignment)
                .pause(1)
                .exec(successCompile, failCompile)
                .repeat(Conf.attemptCount, "i")
                .on(attemptCompileAndTest.pause(session -> Duration.ofSeconds(Conf.waitTimeBetweenSubmits.get())))
                .pause(1)
                .exec(submit)
                .pause(1)
                .exec(ws("Close Websocket").close());
        setUp(scn.injectOpen(rampUsers(Conf.teams).during(Conf.ramp))).protocols(httpProtocol);
    }

    private void appendToFile(long userId, String content) {
        try {
            String filename = "session-" + userId + ".txt";
            Path f = Paths.get("target", "gatling", "logs-" + runId, filename);
            Files.createDirectories(f.getParent());
            Files.writeString(f, content.replace("\0", ""), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            // ignored
        }
    }

    private static User initializeUser(Session session) {
        User user = RestClient.createUser(String.format("%spt-user-%d", Conf.prefix,session.userId()));
        return new User(user.id(), user.username(), user.password(), String.format("%spt-team-%d", Conf.prefix,session.userId()));
    }

    private void parseHtml(String html) {
//        System.out.println("-------------------------------------");
//        System.out.println(html);
//        System.out.println("--------------------------------------");
        // We are looking at the tab components. Each has an 'id' that starts with 'cm-' and then the UUID of the tab.
        String[] split = html.split("id=\"cm-");

        // The amount of tabs should be 9, so the length of the array should be 10 (index 0 is the html before the tabs).
        // Otherwise the wrong assignment might be active (or none)
        if (split.length != Conf.assignment.getTabCount() + 1) {
            //System.out.println(html);
            System.err.println(">>>>>> Is the " + Conf.assignment.getAssignmentName() + " assignment currently running?");
            after();
            System.exit(1);
        }

        // ignore 0, it is the html before the tabs so no UUIDs there
        // ignore 1, it's the uuid of the assignment tab
        // 2 is the SuperAwesomeNameService
        superAwesomeNamingServiceUUID = split[2].substring(0, 36);
        // the rest are all tests
        for (int i = 3; i < split.length; i++) {
            testUUIDs.add(split[i].substring(0, 36));
        }

        sessionUUID = html.split("session = '")[1].substring(0, 36);
    }

    private String getCompileMessage(String code) {
        String content = Json.stringify(Message.builder()
                .sources(Collections.singletonList(new Message.Source(superAwesomeNamingServiceUUID, code)))
                .tests(null)
                .assignmentName(Conf.assignment.getAssignmentName())
                .uuid(sessionUUID)
                .timeLeft("60")
                .arrivalTime(null)
                .build(), false);

        return "SEND\n" + "destination:/app/submit/compile\n" + "content-length:" + content.length() + "\n" + "\n" + content + "\u0000";
    }

    private String getTestMessage(String code) {
        return getTestMessage(code, "test");
    }

    private String getSubmitMessage(String code) {
        return getTestMessage(code, "submit");
    }

    private String getTestMessage(String code, String endpoint) {
        String content = Json.stringify(Message.builder()
                .sources(Collections.singletonList(new Message.Source(superAwesomeNamingServiceUUID, code)))
                .tests(testUUIDs)
                .assignmentName(Conf.assignment.getAssignmentName())
                .uuid(sessionUUID)
                .timeLeft("60")
                .arrivalTime(null)
                .build(), false);

        return "SEND\n" + "destination:/app/submit/" + endpoint + "\n" + "content-length:" + content.length() + "\n" + "\n" + content + "\u0000";
    }

    public static <T> T either(T... options) {
        return options[random(options.length)];
    }

    public static String[] createOptions(int attempts) {
        String[] options = new String[attempts + 1];
        options[0] = Conf.assignment.getEmpty();

        for (int i = 1; i < options.length; i++) {
            options[i] = Conf.assignment.getAttempt(i - 1);
        }
        return options;
    }

    public static int random(int maxExclusive) {
        return (int) (Math.random() * maxExclusive);
    }

    public static int random(int min, int maxExclusive) {
        return (int) (Math.random() * (maxExclusive - min)) + min;
    }

}
