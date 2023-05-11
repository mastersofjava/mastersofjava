package performance;

import io.gatling.core.json.Json;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.regex;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.gatling.javaapi.http.HttpDsl.ws;

public class PerformanceTest extends Simulation {

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://" + Conf.mojServerUrl)
            .header("Authorization", session -> session.get("authToken"))
            .inferHtmlResources()
            .userAgentHeader("Gatling2")
            .wsBaseUrl("ws://" + Conf.mojServerUrl);

    private String superAwesomeNamingServiceUUID;
    private List<String> testUUIDs = new ArrayList<>();
    private String sessionUUID;
    private final ScenarioBuilder scn = getScenario();

    {
        // This is where the test starts:
        setUp(scn.injectOpen(rampUsers(Conf.teams).during(Conf.ramp))).protocols(httpProtocol);
    }

    private ScenarioBuilder getScenario() {

        return scenario("Test " + Conf.assigmentName)
                .exec(session -> {
                    var now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss:SSS"));
                    session = session.set("testUser", "TestUser" + now);
                    session = session.set("testTeam", "Team " + now);
                    session = session.set("authToken", "Bearer " + KeyCloakHelper.createUserAndReturnToken("http://" + Conf.keyCloakUrl, session.get("testUser")));
                    return session;
                })
                .exec(http("Initiate connection")
                        .get("/play")
                        .check(status().is(200))
                )
                .exec(http("Add team")
                        .post("/team")
                        .formParam("name", session -> session.get("testTeam"))
                        .formParam("company", session -> session.get("testTeam"))
                        .formParam("country", session -> session.get("testTeam"))
                        .formParam("submit", "")
                        .check(status().is(200))
                )
                .exec(http("Get assignment info")
                        .get("/play")
                        .check(status().is(200))
                        .check(bodyString().saveAs("assignmentHtml"))
                )
                .exec(session -> {
                    parseHtml(session.getString("assignmentHtml"));
                    return session;
                })

                .exec(ws("Connect WebSocket")
                        .connect("/ws/session/websocket")
                        .onConnected(
                                exec(ws("Connect with Stomp").sendText(
                                                """     
                                                        CONNECT
                                                        accept-version:1.0,1.1,1.2
                                                        heart-beat:4000,4000
                                                                                                                
                                                        \u0000
                                                        """
                                        )
                                        .await(10).on(ws.checkTextMessage("Check connection").check(regex("CONNECTED\n.*"))))
                                        .exec(ws("Subscribe to user destination").sendText(
                                                """
                                                        SUBSCRIBE
                                                        ack:client
                                                        id:sub-0
                                                        destination:/user/queue/session
                                                                                                                
                                                        \u0000
                                                        """
                                        )))
                )
                .pause(1)
                .exec(ws("Compile")
                        .sendText(session -> getCompileMessage(Conf.emptyCode))
                        .await(10).on(
                                ws.checkTextMessage("Compile started")
                                        .check(regex(".*COMPILE.*success\":true.*"))
                        )
                )
                .exec(ws("Compile wrong code")
                                .sendText(session -> getCompileMessage(Conf.doesNotCompile))
                        .await(10).on(
                                ws.checkTextMessage("Compile finished")
                                        .check(regex(".*COMPILE.*success\":false.*"))
                        )
                )
                .repeat(Conf.attemptCount, "i").on(
                        either(
                        exec(ws("Attempt #{i}")
                                        // randomly choose how many UT's will fail.
                                        .sendText(session -> getTestMessage(
                                                        either(
                                                                Conf.emptyCode,
                                                                Conf.attempt1,
                                                                Conf.attempt2,
                                                                Conf.attempt3,
                                                                Conf.attempt4,
                                                                Conf.attempt5
                                                        )))
                                .await(10).on(
                                        ws.checkTextMessage("Testing started")
                                                .check(regex(".*TESTING_STARTED.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Compile success")
                                                .check(regex(".*COMPILE.*success\":true.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Test0 done")
                                                .check(regex(".*Test0.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Test1 done")
                                                .check(regex(".*Test1.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Test2 done")
                                                .check(regex(".*Test2.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Test3 done")
                                                .check(regex(".*Test3.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Test4 done")
                                                .check(regex(".*Test4.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Test5 done")
                                                .check(regex(".*Test5.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Test6 done")
                                                .check(regex(".*Test6.*")))
                        ))
                                .pause(session -> Duration.ofSeconds(Conf.waitTimeBetweenSubmits.get()))
                )
                .pause(1)


                .exec(ws("Submit")
                                .sendText(session -> getSubmitMessage(Conf.correctSolution))
                        .await(10).on(
                                ws.checkTextMessage("Submit started")
                                        .check(regex(".*SUBMIT_STARTED.*")))
                        .await(10).on(
                                ws.checkTextMessage("Compile success")
                                        .check(regex(".*COMPILE.*success.*true.*")))
                        .await(10).on(
                                ws.checkTextMessage("Test0 done")
                                        .check(regex(".*Test0.*")))
                        .await(10).on(
                                ws.checkTextMessage("Hidden test done")
                                        .check(regex(".*HiddenTest.*")))
                        .await(10).on(
                                ws.checkTextMessage("Test1 done")
                                        .check(regex(".*Test1.*")))
                        .await(10).on(
                                ws.checkTextMessage("Test2 done")
                                        .check(regex(".*Test2.*")))
                        .await(10).on(
                                ws.checkTextMessage("Test3 done")
                                        .check(regex(".*Test3.*")))
                        .await(10).on(
                                ws.checkTextMessage("Test4 done")
                                        .check(regex(".*Test4.*")))
                        .await(10).on(
                                ws.checkTextMessage("Test5 done")
                                        .check(regex(".*Test5.*")))
                        .await(10).on(
                                ws.checkTextMessage("Test6 done")
                                        .check(regex(".*Test6.*")))
                        .await(10).on(
                                ws.checkTextMessage("Submit done")
                                        .check(regex(".*SUBMIT.*")))
                )
                .pause(1)

                .exec(ws("Close Websocket").close())
                ;
    }

    private void parseHtml(String html) {
//        System.out.println("-------------------------------------");
//        System.out.println(html);
//        System.out.println("--------------------------------------");
        // We are looking at the tab components. Each has an 'id' that starts with 'cm-' and then the UUID of the tab.
        String[] split = html.split("id=\"cm-");

        // The amount of tabs should be 9, so the length of the array should be 10 (index 0 is the html before the tabs).
        // Otherwise the wrong assignment might be active (or none)
        if (split.length != 10) {
            System.out.println(html);
            System.err.println(">>>>>> Is the " + Conf.assigmentName + " assignment currently running?");
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
                        .assignmentName(Conf.assigmentName)
                        .uuid(sessionUUID)
                        .timeLeft("60")
                        .arrivalTime(null)
                        .build()
                , false);

        return "SEND\n" +
                "destination:/app/submit/compile\n" +
                "content-length:" + content.length() + "\n" +
                "\n" +
                content +
                "\u0000";
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
                        .assignmentName(Conf.assigmentName)
                        .uuid(sessionUUID)
                        .timeLeft("60")
                        .arrivalTime(null)
                        .build()
                , false);

        return "SEND\n" +
                "destination:/app/submit/" + endpoint + "\n" +
                "content-length:" + content.length() + "\n" +
                "\n" +
                content +
                "\u0000";
    }

    public static <T> T either(T... options) {
        return options[random(options.length)];
    }

    public static int random(int maxExclusive) {
        return (int) (Math.random() * maxExclusive);
    }

    public static int random(int min, int maxExclusive) {
        return (int) (Math.random() * (maxExclusive - min)) + min;
    }

}
