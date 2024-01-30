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
package nl.moj.common.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.common.storage.StorageService;
import nl.moj.modes.Mode;

@Service
@Slf4j
@AllArgsConstructor
public class BootstrapService {

    private MojServerProperties mojServerProperties;

    private StorageService storageService;

    private static String[] LIBS = {
            "asciiart-core-1.1.0.jar",
            "hamcrest-all-1.3.jar",
            "junit-4.12.jar",
            "securityPolicyForUnitTests.policy"
    };

    private static String[] SOUNDS = {
            "gong.wav",
            "clock-tick.mp3"
    };

    public boolean isBootstrapNeeded() {
        return !dataValid();
    }

    public void bootstrap() throws IOException {
        storageService.initStorage();
        if (mojServerProperties.getMode().anyMatch(Mode.CONTROLLER, Mode.SINGLE)) {
            copyLibs();
            copySounds();
        }
        if (mojServerProperties.getMode() == Mode.WORKER) {
            copyLibs();
        }
    }

    private void copyLibs() throws IOException {
        log.info("Populating needed libs.");
        Path libs = storageService.getLibsFolder();
        for (String lib : LIBS) {
            Files.copy(getClass().getResourceAsStream("/bootstrap/libs/" + lib), libs.resolve(lib),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copySounds() throws IOException {
        log.info("Populating sounds.");
        Path sounds = storageService.getSoundsFolder();
        for (String sound : SOUNDS) {
            Files.copy(getClass().getResourceAsStream("/bootstrap/sounds/" + sound), sounds.resolve(sound),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean libsValid() {
        return Arrays.stream(LIBS).allMatch(l -> Files.isReadable(storageService.getLibsFolder().resolve(l)));
    }

    private boolean soundsValid() {
        return Arrays.stream(SOUNDS).allMatch(l -> Files.isReadable(storageService.getSoundsFolder().resolve(l)));
    }

    private boolean dataValid() {
        if (mojServerProperties.getMode().anyMatch(Mode.CONTROLLER, Mode.SINGLE)) {
            return libsValid() && soundsValid();
        }
        if (mojServerProperties.getMode() == Mode.WORKER) {
            return libsValid();
        }
        return false;
    }

}
