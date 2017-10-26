package nl.moj.server.compile;

import nl.moj.server.SubmitController;
import nl.moj.server.TaskControlController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompileServiceTest {

    @Autowired
    private TaskControlController taskControlController;

    @Autowired
    private CompileService compileService;

    @Test
    public void doesNotCompileUncompilableCode() {

        taskControlController.cloneAssignmentsRepo();
        taskControlController.startTask(new TaskControlController.TaskMessage("VirtualCPU"));

        Map<String, String> source = new HashMap<>();
        source.put("VirtualCPU", "");

        compileService.compile(new SubmitController.SourceMessage("test_team", source, Collections.emptyList()), false, false);
    }

}
