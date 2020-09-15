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
import java.util.UUID;

import lombok.AllArgsConstructor;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ApplicationEventListener implements ApplicationListener<ContextRefreshedEvent> {

    private final CompetitionRuntime competitionRuntime;
    private final CompetitionRepository competitionRepository;
    private final MojServerProperties mojServerProperties;

    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        initCompetition();
    }

    private void initCompetition() {
        if (competitionRuntime.getCompetition() == null) {
            // Goal: pick with a competition to start with (this can be configured)
            // - if database empty, create the default configured competition
            // - if configured competition not available anymore then pick the first competition.
            UUID competitionUuid = mojServerProperties.getCompetition().getUuid();
            Competition c = competitionRepository.findByUuid(competitionUuid);
            boolean isEmptyDatabase = competitionRepository.count()==0;

            if (c == null && isEmptyDatabase) {
                c = new Competition();
                c.setUuid(competitionUuid);
                c.setName("Masters of Java");
                c = competitionRepository.save(c);
            } else
            if (c==null ) {
                c = competitionRepository.findAll().get(0);
            }
            competitionRuntime.loadMostRecentSession(c);
        }
    }
}
