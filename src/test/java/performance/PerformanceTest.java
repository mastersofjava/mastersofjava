package performance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import io.gatling.core.json.Json;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import nl.moj.server.submit.model.SourceMessage;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

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

    private ScenarioBuilder scn;

    {
        scn = scenario("Test Assignment Hell")
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

                .exec(ws("Connect WebSocket").connect("/ws/competition/websocket"))
                .pause(1)
                .exec(ws("Compile")
                        .sendText(session -> getCompileMessage(Conf.emptyCode))
                        .await(30).on(
                                ws.checkTextMessage("Compile succes").check(regex("(.|\n)*"))
                        ))

//                                        .matching(jsonPath("$.messageType").is("COMPILE"))
//                                        .check(jsonPath("$.success").ofBoolean().is(true))))

//                .pause(1)
//                .exec(ws("Test Empty code")
//                                .sendText(session -> getTestMessage(Conf.emptyCode))
//                )
//                .pause(1)
//                .exec(ws("Test correct solution")
//                                .sendText(session -> getTestMessage(Conf.correctSolution))
//                )
//                .pause(1)
                .exec(ws("Close Websocket").close())
        ;
        setUp(scn.injectOpen(rampUsers(Conf.users).during(Conf.ramp))).protocols(httpProtocol);
    }

    private void parseHtml(String html) {
        // We are looking at the tab components. Each has an 'id' that starts with 'cm-' and then the UUID of the tab.
        String[] split = html.split("id=\"cm-");

        // The amount of tabs should be 9, so the length of the array should be 10. Otherwise the wrong assignment might be active (or none)
        if (split.length != 10) {
            throw new RuntimeException("Is the correct assignment currently running?");
        }

        // ignore 0, no uuid's there
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
                        .assignmentName("requirement hell")
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
        String content = Json.stringify(Message.builder()
                        .sources(Collections.singletonList(new Message.Source(superAwesomeNamingServiceUUID, code)))
                        .tests(testUUIDs)
                        .assignmentName("requirement hell")
                        .uuid(sessionUUID)
                        .timeLeft("60")
                        .arrivalTime(null)
                        .build()
                , false);

        return "SEND\n" +
                "destination:/app/submit/test\n" +
                "content-length:" + content.length() + "\n" +
                "\n" +
                content +
                "\u0000";
    }
}
