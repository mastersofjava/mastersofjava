package nl.moj.server.compiler;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nl.moj.server.TaskControlController;

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
