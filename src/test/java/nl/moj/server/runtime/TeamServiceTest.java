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

import nl.moj.server.competition.model.CompetitionAssignment;
import nl.moj.server.competition.service.CompetitionServiceException;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.teams.service.TeamService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;

@SpringBootTest
public class TeamServiceTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private TeamService teamService;


    private static Stream<String> assignments() {
        return Stream.of("sequential", "parallel");
    }

    private void startSelectedAssignment(String assignment) {
        try {
            CompetitionAssignment oa = getAssignment(assignment);
            competitionRuntime.startAssignment(competitionRuntime.getSessionId(), oa.getAssignment()
                    .getUuid());
        } catch (CompetitionServiceException cse) {
            throw new RuntimeException(cse);
        }
    }

    @ParameterizedTest
    @MethodSource("assignments")
    public void invisibleTestsShouldHaveContentIntentionallyHidden(String assignment) throws Exception {
        startSelectedAssignment(assignment);
        List<AssignmentFile> files = teamService.getTeamAssignmentFiles(getTeam().getUuid(),
                competitionRuntime.getSessionId(),
                competitionRuntime.getActiveAssignment(null).getAssignment().getUuid());

        Assertions.assertThat(files).hasSize(6);
        Assertions.assertThat(files.stream().filter(f -> f.getFileType() == AssignmentFileType.INVISIBLE_TEST)
                .allMatch(f -> f.getContentAsString().equals("-- content intentionally hidden --"))).isTrue();
    }
}
