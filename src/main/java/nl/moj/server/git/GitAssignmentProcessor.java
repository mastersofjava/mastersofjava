package nl.moj.server.git;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GitAssignmentProcessor {

    @Qualifier("yamlObjectMapper")
    private final ObjectMapper yamlObjectMapper;

    public void process(Resource resource) throws IOException {

        GitAssignment gitAssignment = yamlObjectMapper.readValue(resource.getInputStream(), GitAssignment.class);

        System.out.println(gitAssignment);

    }

}
