package nl.moj.server.bootstrap;

import nl.moj.server.bootstrap.filter.BootstrapFilter;
import nl.moj.server.bootstrap.service.BootstrapService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BootstrapFilterTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @MockBean
    private BootstrapService bootstrapService;

    @Before
    public void before() {
        BootstrapFilter bf = new BootstrapFilter(bootstrapService);
        bf.setServletContext(wac.getServletContext());

        mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .addFilter(bf)
                .build();
    }

    @Test
    public void shouldRedirectToBootstrap() throws Exception {
        when(bootstrapService.isBootstrapNeeded()).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.get("/")).andExpect(MockMvcResultMatchers.redirectedUrl("/bootstrap"));
    }
}
