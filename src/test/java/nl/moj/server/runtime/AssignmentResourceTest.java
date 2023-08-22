package nl.moj.server.runtime;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.competition.model.CompetitionAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public class AssignmentResourceTest extends BaseRuntimeTest {

    @Autowired
    private CompetitionRuntime competitionRuntime;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testOk() throws Exception {
        CompetitionAssignment oa = getAssignment("parallel");
        competitionRuntime.startAssignment(competitionRuntime.getSessionId(), oa.getAssignment()
                .getUuid());

        mockMvc.perform(MockMvcRequestBuilders.get("/public/asset/{assignment}/{file}", oa.getAssignment()
                        .getName(), "assets/images/icon.png"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType("image/png"));
    }

    @Test
    public void testNonExisting() throws Exception {
        CompetitionAssignment oa = getAssignment("parallel");
        competitionRuntime.startAssignment(competitionRuntime.getSessionId(), oa.getAssignment()
                .getUuid());

        mockMvc.perform(MockMvcRequestBuilders.get("/public/asset/{assignment}/{file}", oa.getAssignment()
                        .getName(), "assets/images/unknown.png"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
