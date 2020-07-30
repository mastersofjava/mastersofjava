package nl.moj.server.harnas;

import nl.moj.server.TaskControlController;
import nl.moj.server.bootstrap.filter.BootstrapFilter;
import nl.moj.server.runtime.BaseRuntimeTest;
import nl.moj.server.teams.repository.TeamRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore
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


    @Before
    public void before() {

        mockMvc =  MockMvcBuilders
                .webAppContextSetup(this.wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();


    }

    @Ignore
    @Test
    public void shouldWhen() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                .param("username", "admin")
                .param("password", "admin")
        );///.andExpect(MockMvcResultMatchers.redirectedUrl("/control"));
    }


}
