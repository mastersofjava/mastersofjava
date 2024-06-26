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
package nl.moj.server;

import javax.transaction.Transactional;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import nl.moj.common.config.properties.MojServerProperties;
import nl.moj.modes.Mode;
import nl.moj.server.runtime.CompetitionRuntime;

@Component
@AllArgsConstructor
public class ServerStartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private final MojServerProperties mojServerProperties;

    private final CompetitionRuntime competitionRuntime;

    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        continueMostRecentSession();
    }

    // TODO this should be removed. Admin should create/start/continue sessions.
    private void continueMostRecentSession() {
        if (mojServerProperties.getMode().anyMatch(Mode.SINGLE, Mode.CONTROLLER)) {
            if (competitionRuntime.getCompetition() == null) {
                competitionRuntime.loadMostRecentSession();
            }
        }
    }
}
