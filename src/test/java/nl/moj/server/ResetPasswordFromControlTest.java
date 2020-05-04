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
package nl.moj.server;

import java.util.UUID;

import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ResetPasswordFromControlTest {

    private static final String OLD_PASSWORD = "schaap";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamRepository teamRepository;

    private Team team;

    @Before
    public void SetUp() {
        team = new Team();
        team.setUuid(UUID.randomUUID());
        team.setPassword(OLD_PASSWORD);

        when(teamRepository.findByUuid(Mockito.any(UUID.class))).thenReturn(team);
        when(teamRepository.save(Mockito.any(Team.class))).thenReturn(team);
    }

    @Test
    public void happyFlow() throws Exception {

        var request = new TaskControlController.NewPasswordRequest();
        request.setTeamUuid(team.getUuid().toString());
        request.setNewPassword("password");
        request.setNewPasswordCheck("password");

        mockMvc.perform(post("/control/resetPassword").flashAttr("newPasswordRequest", request))//
                .andExpect(redirectedUrl("/control"))//
                .andExpect(flash().attributeExists("success"));
        // password is changed and encrypted
        assertNotEquals(OLD_PASSWORD, team.getPassword());
        assertNotEquals("password", team.getPassword());
    }

    @Test
    public void passwordsNotMatching() throws Exception {
        var request = new TaskControlController.NewPasswordRequest();
        request.setTeamUuid(team.getUuid().toString());
        request.setNewPassword("password");
        request.setNewPasswordCheck("a different password");

        mockMvc.perform(post("/control/resetPassword").flashAttr("newPasswordRequest", request))//
                .andExpect(redirectedUrl("/control"))//
                .andExpect(flash().attributeExists("error"))//
                .andExpect(flash().attribute("newPasswordRequest", request));

        assertNull(request.getNewPassword());
        assertNull(request.getNewPasswordCheck());

        assertEquals("When providing non-matching passwords, the password should not change", OLD_PASSWORD,
                team.getPassword());
    }

    @Test
    public void noTeamSelectedNothingHappens() throws Exception {
        var request = new TaskControlController.NewPasswordRequest();
        request.setTeamUuid("0");
        request.setNewPassword("password");
        request.setNewPasswordCheck("a different password");

        mockMvc.perform(post("/control/resetPassword").flashAttr("newPasswordRequest", request))//
                .andExpect(redirectedUrl("/control"))//
                .andExpect(flash().attributeExists("error"))//
                .andExpect(flash().attribute("newPasswordRequest", request));

        assertNull(request.getNewPassword());
        assertNull(request.getNewPasswordCheck());

        assertEquals("When selecting no team, no password is changed", OLD_PASSWORD, team.getPassword());
    }

    @Test
    public void noPasswordTyped() throws Exception {
        var request = new TaskControlController.NewPasswordRequest();
        request.setTeamUuid(team.getUuid().toString());
        request.setNewPassword("  ");
        request.setNewPasswordCheck("  ");

        mockMvc.perform(post("/control/resetPassword").flashAttr("newPasswordRequest", request))//
                .andExpect(redirectedUrl("/control"))//
                .andExpect(flash().attributeExists("error"))//
                .andExpect(flash().attribute("newPasswordRequest", request));

        assertNull(request.getNewPassword());
        assertNull(request.getNewPasswordCheck());

        assertEquals("When provinding an empty password, the password should not change", OLD_PASSWORD,
                team.getPassword());
    }
}
