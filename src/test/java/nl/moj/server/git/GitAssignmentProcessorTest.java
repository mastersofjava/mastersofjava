package nl.moj.server.git;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GitAssignmentProcessorTest {

    @Autowired
    private GitAssignmentProcessor gitAssignmentProcessor;

    @Test
    public void process() throws IOException {

        Resource resource = new ClassPathResource("assignment-example.yml");

        gitAssignmentProcessor.process(resource);

    }
}