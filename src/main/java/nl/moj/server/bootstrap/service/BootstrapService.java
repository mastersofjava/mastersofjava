/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.bootstrap.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.config.properties.Directories;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.repository.TeamRepository;

@Service
@Slf4j
@AllArgsConstructor
public class BootstrapService {

    private MojServerProperties mojServerProperties;

    private static String[] LIBS = {
            "asciiart-core-1.1.0.jar",
            "hamcrest-all-1.3.jar",
            "junit-4.12.jar",
            "securityPolicyForUnitTests.policy"
    };

    private static String[] SOUNDS = {
            "gong.wav",
            "slowtictac.wav",
            "slowtictaclong.wav",
            "tictac1.wav",
            "tictac2.wav",
            "tikking.wav"
    };


    public boolean isBootstrapNeeded() {
        return !dataValid();
    }

    public void bootstrap() throws IOException {
        updateData();
    }

    private void updateData() throws IOException {
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
        for (String lib : LIBS) {
            Files.copy(getClass().getResourceAsStream("/bootstrap/libs/" + lib), libs.resolve(lib), StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Populating sounds.");
        Path sounds = directories.getBaseDirectory().resolve(directories.getSoundDirectory());
        for (String sound : SOUNDS) {
            Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/" + sound), sounds.resolve(sound), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean dataValid() {
        Directories directories = mojServerProperties.getDirectories();
        Path libs = directories.getBaseDirectory().resolve(directories.getLibDirectory());
        boolean valid = true;
        for (String lib : LIBS) {
            valid = valid && libs.resolve(lib).toFile().exists();
        }

        Path sounds = directories.getBaseDirectory().resolve(directories.getSoundDirectory());
        for (String sound : SOUNDS) {
            valid = valid && sounds.resolve(sound).toFile().exists();
        }
        return valid;
    }

}
