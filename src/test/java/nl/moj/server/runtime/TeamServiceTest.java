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
package nl.moj.server.runtime;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitRequest;
import nl.moj.server.submit.service.SubmitResult;
import nl.moj.server.submit.service.SubmitService;
import nl.moj.server.teams.service.TeamService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TeamServiceTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private TeamService teamService;


    private static Stream<String> assignments() {
        return Stream.of("sequential","parallel");
    }

    private void startSelectedAssignmment(String assignment) {
        OrderedAssignment oa = getAssignment(assignment);

        competitionRuntime.startAssignment(oa.getAssignment().getName());
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void invisibleTestsShouldHaveContentIntentionallyHidden(String assignment) throws Exception {
        startSelectedAssignmment(assignment);
        List<AssignmentFile> files = teamService.getTeamAssignmentFiles(competitionRuntime.getCompetitionSession(),
                competitionRuntime.getActiveAssignment().getAssignment(),getTeam());

        Assertions.assertThat(files).hasSize(6);
        Assertions.assertThat(files.stream().filter( f -> f.getFileType() == AssignmentFileType.INVISIBLE_TEST )
                .allMatch( f -> f.getContentAsString().equals("-- content intentionally hidden --"))).isTrue();
    }
}
