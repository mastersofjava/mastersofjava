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
package nl.moj.server.login;

import lombok.RequiredArgsConstructor;
import nl.moj.server.competition.service.CompetitionService;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Deprecated
@Controller
@RequiredArgsConstructor
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final TeamRepository teamRepository;

    private final CompetitionService competitionService;

    private boolean isRegistrationFormDisabled() {
        Team team = teamRepository.findByName("admin");
        log.info("isRegistrationFormDisabled {}", team.getCompany());
        return team.getCompany().contains("HIDE_REGISTRATION");
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        boolean isDisabled = isRegistrationFormDisabled();
        model.addAttribute("registration_disabled", isDisabled);
        return "login";
    }

    @PostMapping("/register")
    public String registerSubmit(Model model, @ModelAttribute("form") SignupForm form) {
        boolean isDisabled = isRegistrationFormDisabled();
        model.addAttribute("registration_disabled", isDisabled);
        if (isDisabled) {
            return "redirect:/logout";
        }

        if (StringUtils.isBlank(form.getName())) {
            model.addAttribute("errors", "Not all fields are filled in");
            return "register";
        }
        if (form.getName().length() > 50 || form.getCompany().length() > 50) {
            model.addAttribute("errors", "To many characters (at least 1, max 50) in team or company name");
            return "register";
        }
        if (teamRepository.findByName(form.getName()) != null) {
            model.addAttribute("errors", "Name already in use");
            return "register";
        }
        String role = Role.USER;
        if (!Role.USER.equals(form.getRole())) {
            role = Role.GAME_MASTER;
        }
        competitionService.createNewTeam(form, role);
        return "redirect:/";
    }


    @GetMapping("/register")
    public String registrationForm(Model model) {
        boolean isDisabled = isRegistrationFormDisabled();
        log.info("registrationForm.isDisabled "+ isDisabled);
        model.addAttribute("registration_disabled", isDisabled);
        if (isDisabled) {
            return "redirect:/logout";
        }
        model.addAttribute("form", new SignupForm());
        return "register";
    }

}
