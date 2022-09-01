package nl.moj.server.harnas;

import nl.moj.server.TaskControlController;
import nl.moj.server.runtime.BaseRuntimeTest;
import nl.moj.server.teams.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;

@SpringBootTest
@Disabled
public class TaskControllerTest extends BaseRuntimeTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private TeamRepository teamRepository;

    private MockMvc mockMvc;
    @Resource
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private TaskControlController taskControlController;


    @BeforeEach
    public void before() {

        mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();


    }

    @Disabled
    @Test
    public void shouldWhen() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                .param("username", "admin")
                .param("password", "admin")
        );///.andExpect(MockMvcResultMatchers.redirectedUrl("/control"));
    }


}
