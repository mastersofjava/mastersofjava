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
package nl.moj.server.rankings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.rankings.model.Ranking;
import nl.moj.server.rankings.service.RankingsService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.CompetitionState;
import nl.moj.server.util.CollectionUtil;
import nl.moj.server.util.HttpUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RankingsController {

    private final CompetitionRuntime competitionRuntime;

    private final RankingsService rankingsService;

    @GetMapping("/rankings")
    public ModelAndView getRankings() {
        CompetitionRuntime rankingProvider = competitionRuntime;
        log.info("session " + HttpUtil.getParam("session") + " - " + rankingProvider);

        Competition competition = rankingProvider.getCompetition();
        List<Ranking> rankings = enrich(rankingsService.getRankings(rankingProvider.getCompetitionSession()));
        CompetitionState competitionState = rankingProvider.getCompetitionState();
        ModelAndView model = new ModelAndView("rankings");
        if (competitionState.getCompletedAssignments().isEmpty()) {
            model.addObject("oas", Collections.emptyList());
        } else {
            model.addObject("oas", rankingsService.getRankingHeaders(competitionState));
        }
        model.addObject("top", rankings.subList(0, Math.min(5, rankings.size())));

        List<List<Ranking>> parts = partitionRemaining(rankings, 5);
        model.addObject("competitionName", competition.getShortName());
        model.addObject("bottom1", parts.get(0));
        model.addObject("bottom1", parts.get(0));
        model.addObject("bottom2", parts.get(1));
        model.addObject("bottom3", parts.get(2));
        model.addObject("bottom4", parts.get(3));
        if (rankingProvider.getActiveAssignment() != null) {
            ActiveAssignment state = rankingProvider.getActiveAssignment();
            model.addObject("assignment", state.getAssignmentDescriptor().getDisplayName());
            model.addObject("timeLeft", state.getSecondsRemaining());
            model.addObject("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addObject("running", state.isRunning());
        } else {
            model.addObject("assignment", "-");
            model.addObject("timeLeft", 0);
            model.addObject("time", 0);
            model.addObject("running", false);
        }
        return model;
    }

    private List<List<Ranking>> partitionRemaining(List<Ranking> rankings, int offset) {
        List<Ranking> remaining = new ArrayList<>();
        if (rankings.size() > offset) {
            remaining = rankings.subList(offset, rankings.size());
        }
        return CollectionUtil.partition(remaining, 4);
    }

    private List<Ranking> enrich(List<Ranking> rankings) {
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }
        return rankings;
    }
}
