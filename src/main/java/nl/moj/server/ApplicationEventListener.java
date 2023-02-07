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

import lombok.AllArgsConstructor;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ApplicationEventListener implements ApplicationListener<ContextRefreshedEvent> {

    private final CompetitionRuntime competitionRuntime;

    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        continueMostRecentSession();
    }

    // TODO this should be removed. Admin should create/start/continue sessions.
    private void continueMostRecentSession() {
        if (competitionRuntime.getCompetition() == null) {
            competitionRuntime.loadMostRecentSession();
        }
    }
}
