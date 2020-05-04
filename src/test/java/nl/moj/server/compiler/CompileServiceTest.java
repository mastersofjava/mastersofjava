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
package nl.moj.server.compiler;

import java.util.HashMap;
import java.util.Map;

import nl.moj.server.TaskControlController;
import nl.moj.server.compiler.service.CompileService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompileServiceTest {

    @Autowired
    private TaskControlController taskControlController;

    @Autowired
    private CompileService compileService;

    @Test
    @Ignore
    public void doesNotCompileUncompilableCode() {

        //taskControlController.scanAssignments("practice-assignments");
        taskControlController.startTask(new TaskControlController.TaskMessage("VirtualCPU"));

        Map<String, String> source = new HashMap<>();
        source.put("VirtualCPU", "");

        //compileService.compile(new SubmitController.SourceMessage("test_team", source, Collections.emptyList()), false, false, 100);
    }

}
