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
        if( competitionRuntime.getCompetition() == null ) {
            UUID competitionUuid = mojServerProperties.getCompetition().getUuid();
            Competition c = competitionRepository.findByUuid(competitionUuid);

            if (c == null) {
                c = new Competition();
                c.setUuid(competitionUuid);
                c.setName("Masters of Java");
                c = competitionRepository.save(c);
            }
            competitionRuntime.loadMostRecentSession(c);
        }
    }
}
