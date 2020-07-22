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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.submit.SubmitResult;
import nl.moj.server.submit.model.SourceMessage;
import nl.moj.server.submit.service.SubmitService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomSecurityPolicyTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private SubmitService submitService;

    @Autowired
    private MojServerProperties mojServerProperties;

    @Test
    public void shouldUseAssignmentSecurityPolicy() throws Exception {

        OrderedAssignment oa = getAssignment("custom-security-policy");

        competitionRuntime.startAssignment(oa.getAssignment().getName());

        SubmitResult submitResult = doSubmitValidInput();

        Assertions.assertThat(submitResult.isSuccess()).isTrue();
        Assertions.assertThat(submitResult.getTestResults().getResults().get(0).isSuccess()).isTrue();
        Assertions.assertThat(submitResult.getTestResults().getResults().get(0).isTimeout()).isFalse();
    }

    private SubmitResult doSubmitValidInput() throws Exception  {
        ActiveAssignment state = competitionRuntime.getActiveAssignment();
        Duration timeout = state.getAssignmentDescriptor().getTestTimeout();
        timeout = timeout.plus(mojServerProperties.getLimits().getCompileTimeout());

        Map<String, String> files = state.getAssignmentFiles().stream()
                .filter(f -> f.getFileType() == AssignmentFileType.EDIT)
                .collect(Collectors.toMap(f -> f.getUuid().toString(), AssignmentFile::getContentAsString));

        SourceMessage src = new SourceMessage();
        src.setSources(files);
        src.setTests(List.of(state.getTestFiles().get(0).getUuid().toString()));


        return submitService.test(getTeam(), src)
                .get(timeout.plusSeconds(10).toSeconds(), TimeUnit.SECONDS);
    }
}
