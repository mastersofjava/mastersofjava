package performance;

import io.gatling.core.json.Json;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.regex;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.gatling.javaapi.http.HttpDsl.ws;

public class PerformanceTest extends Simulation {

    private final String testUser;
    private final String testTeam;
    private final String userToken;

    {
        var now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss:SSS"));
        testUser = "TestUser" + now;
        testTeam = "Team " + now;
        userToken = KeyCloakHelper.createUserAndReturnToken("http://" + Conf.keyCloakUrl, testUser);
    }

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://" + Conf.mojServerUrl)
            .inferHtmlResources()
            .userAgentHeader("Gatling2")
            .wsBaseUrl("ws://" + Conf.mojServerUrl);

    private final Map<CharSequence, String> loginHeaders = Map.ofEntries(
            Map.entry("Authorization", "Bearer " + userToken)
    );

    private String superAwesomeNamingServiceUUID;
    private List<String> testUUIDs = new ArrayList<>();
    private String sessionUUID;

    private final ScenarioBuilder scn = getScenario();
    {
        // This is where the test starts:
        setUp(scn.injectOpen(rampUsers(Conf.users).during(Conf.ramp))).protocols(httpProtocol);
    }

    private ScenarioBuilder getScenario() {
        return scenario("Test " + Conf.assigmentName)
                .exec(
                        http("Initiate connection and login automatically")
                                .get("/play")
                                .headers(loginHeaders)
                                .check(status().is(200))
                )
                .exec(
                        http("Add team " + testTeam)
                                .post("/team")
                                .formParam("name", testTeam)
                                .formParam("company", testTeam)
                                .formParam("country", testTeam)
                                .formParam("submit", "")
                                .check(status().is(200))
                )
                .exec(
                        http("Get assignment info")
                                .get("/play")
                                .check(status().is(200))
                                .check(bodyString().saveAs("assignmentHtml"))
                )
                .exec(session -> {
                    parseHtml(session.getString("assignmentHtml"));
                    return session;
                })

                .exec(ws("Connect WebSocket")
                        .connect("/ws/competition/websocket")
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
                                                        destination:/user/queue/competition
                                                                                                                
                                                        \u0000
                                                        """
                                        )))
                )
                .pause(1)
                .exec(ws("Compile")
                        .sendText(session -> getCompileMessage(Conf.emptyCode))
                        .await(10).on(
                                ws.checkTextMessage("Compile started")
                                        .check(regex(".*COMPILING_STARTED.*"))
                        )
                        .await(10).on(
                                ws.checkTextMessage("Compile success")
                                        .check(regex(".*COMPILE.*success.*true.*"))
                        )
                        .await(10).on(
                                ws.checkTextMessage("Compile ended with success")
                                        .check(regex(".*COMPILING_ENDED.*success.*true.*"))
                        )
                )
                .exec(ws("Compile wrong code")
                        .sendText(session -> getCompileMessage(Conf.missingReturnStatement))
                        .await(10).on(
                                ws.checkTextMessage("Compile started")
                                        .check(regex(".*COMPILING_STARTED.*"))
                        )
                        .await(10).on(
                                ws.checkTextMessage("Compile success")
                                        .check(regex(".*COMPILE.*success.*false.*missing return statement.*"))
                        )
                        .await(10).on(
                                ws.checkTextMessage("Compile ended with success")
                                        .check(regex(".*COMPILING_ENDED.*success.*false.*"))
                        )
                )
                //                .exec(session -> {
//                    System.out.println("+++++++++++++++++++ " + session.get("test"));
//                    return session;
//                })
//              .pause(1)
                .repeat(Conf.attemptCount, "i").on(
                        exec(ws("Attempt #{i}")
                                .sendText(session -> getTestMessage(either(
                                        Conf.emptyCode,
                                        Conf.attempt1,
                                        Conf.attempt2,
                                        Conf.attempt3,
                                        Conf.attempt4,
                                        Conf.attempt5,
                                        Conf.correctSolution
                                )))
                                .await(10).on(
                                        ws.checkTextMessage("Testing started")
                                                .check(regex(".*TESTING_STARTED.*")))
                                .await(10).on(
                                        ws.checkTextMessage("Compile success")
                                                .check(regex(".*COMPILE.*success.*true.*")))
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
                                .await(10).on(
                                        ws.checkTextMessage("Testing ended")
                                                .check(regex(".*TESTING_ENDED.*")))
                        )
                                .pause(random(10,100))
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
                .await(10).on(
                        ws.checkTextMessage("Submit ended")
                                .check(regex(".*SUBMIT_ENDED.*")))
        )
                .pause(1)

                .exec(ws("Close Websocket").close())
        ;
    }

    private void parseHtml(String html) {
        // We are looking at the tab components. Each has an 'id' that starts with 'cm-' and then the UUID of the tab.
        String[] split = html.split("id=\"cm-");

        // The amount of tabs should be 9, so the length of the array should be 10 (index 0 is the html before the tabs).
        // Otherwise the wrong assignment might be active (or none)
        if (split.length != 10) {
            System.err.println(">>>>>> Is the "+ Conf.assigmentName +" assignment currently running?");
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

        // The session UUID is in the html, I believe by accident. Eitherway..
        sessionUUID = html.split("submit\\(\\)\" title=\"")[1].substring(0, 36);
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
