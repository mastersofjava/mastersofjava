package nl.moj.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.modes.Mode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private static final String SESSIONS = "sessions";
    private static final String TEAMS = "teams";
    private static final String LIBS = "lib";
    private static final String SOUNDS = "sounds";
    private static final String JAVADOC = "javadoc";
    private static final String ASSIGNMENTS = "assignments";

    private final MojServerProperties mojServerProperties;

    public Path getSessionsFolder() {
        return mojServerProperties.getDataDirectory().resolve(SESSIONS);
    }

    public Path getTeamsFolder(UUID sessionId) {
        return mojServerProperties.getDataDirectory().resolve(sessionId.toString()).resolve(TEAMS);
    }

    public Path getLibsFolder() {
        return mojServerProperties.getDataDirectory().resolve(LIBS);
    }

    public Path getSoundsFolder() {
        return mojServerProperties.getDataDirectory().resolve(SOUNDS);
    }

    public Path getAssignmentsFolder() {
        return mojServerProperties.getDataDirectory().resolve(ASSIGNMENTS);
    }

    public Path getJavadocFolder() {
        return mojServerProperties.getDataDirectory().resolve(JAVADOC);
    }

    public Path getSessionTeamFolder(UUID sessionId,UUID teamId) {
        return getSessionsFolder()
                .resolve(sessionId.toString())
                .resolve(TEAMS)
                .resolve(teamId.toString());
    }

    public void initStorage() throws IOException {
        log.info("Initializing storage.");
        Files.createDirectories(mojServerProperties.getDataDirectory());

        if (mojServerProperties.getMode().anyMatch(Mode.CONTROLLER, Mode.SINGLE)) {
            Files.createDirectories(getSessionsFolder());
            Files.createDirectories(getLibsFolder());
            Files.createDirectories(getSoundsFolder());
            Files.createDirectories(getJavadocFolder());
            Files.createDirectories(getAssignmentsFolder());
        }
        if (mojServerProperties.getMode().anyMatch(Mode.WORKER)) {
            Files.createDirectories(getLibsFolder());
        }
    }
}
