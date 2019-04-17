package nl.moj.server;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.Directories;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.util.RandomString;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class BootstrapService {

    private MojServerProperties mojServerProperties;
    private TeamRepository teamRepository;
    private PasswordEncoder passwordEncoder;


    public void bootstrap() throws IOException {
        Directories directories = mojServerProperties.getDirectories();
        log.info("Initializing data directory. Using base {}.", directories.getBaseDirectory());
        if (!directories.getBaseDirectory().toFile().exists()) {
            directories.getBaseDirectory().toFile().mkdirs();
        }
        if (!directories.getBaseDirectory().resolve(directories.getTeamDirectory()).toFile().exists()) {
            directories.getBaseDirectory().resolve(directories.getTeamDirectory()).toFile().mkdirs();
        }
        if (!directories.getBaseDirectory().resolve(directories.getLibDirectory()).toFile().exists()) {
            directories.getBaseDirectory().resolve(directories.getLibDirectory()).toFile().mkdirs();
        }
        if (!directories.getBaseDirectory().resolve(directories.getSoundDirectory()).toFile().exists()) {
            directories.getBaseDirectory().resolve(directories.getSoundDirectory()).toFile().mkdirs();
        }

        // populate sounds and lib
        log.info("Populating needed libs.");
        Path libs = directories.getBaseDirectory().resolve(directories.getLibDirectory());
        Files.copy(getClass().getResourceAsStream("/bootstrap/libs/asciiart-core-1.1.0.jar"), libs.resolve("asciiart-core-1.1.0.jar"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/libs/hamcrest-all-1.3.jar"), libs.resolve("hamcrest-all-1.3.jar"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/libs/junit-4.12.jar"), libs.resolve("junit-4.12.jar"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/libs/securityPolicyForUnitTests.policy"), libs.resolve("securityPolicyForUnitTests.policy"), StandardCopyOption.REPLACE_EXISTING);

        log.info("Populating sounds.");
        Path sounds = directories.getBaseDirectory().resolve(directories.getSoundDirectory());
        Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/gong.wav"), sounds.resolve("gong.wav"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/slowtictac.wav"), sounds.resolve("slowtictac.wav"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/slowtictaclong.wav"), sounds.resolve("slowtictaclong.wav"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/tictac1.wav"), sounds.resolve("tictac1.wav"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/tictac2.wav"), sounds.resolve("tictac2.wav"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/tikking.wav"), sounds.resolve("tikking.wav"), StandardCopyOption.REPLACE_EXISTING);

        // create a new control user
        String pass = new RandomString(8).nextString();
        Team c = teamRepository.findByName("control");
        if (c != null) {
            teamRepository.delete(c);
        }
        c = Team.builder()
                .company("First8")
                .name("control")
                .uuid(UUID.randomUUID())
                .country("NL")
                .role(Role.ROLE_CONTROL)
                .password(passwordEncoder.encode(pass))
                .build();

        teamRepository.save(c);
        log.info("Created a new control user with username '{}' and password '{}'", "control", pass);
    }
}
