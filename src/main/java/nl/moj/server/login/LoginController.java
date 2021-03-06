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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.teams.model.Role;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final TeamRepository teamRepository;

    private final PasswordEncoder encoder;

    private final MojServerProperties mojServerProperties;

    @GetMapping("/login")
    public String loginForm(Model model) {
        return "login";
    }

    @PostMapping("/register")
    public String registerSubmit(Model model, @ModelAttribute("form") SignupForm form) {
        if (StringUtils.isBlank(form.getName()) || StringUtils.isBlank(form.getPassword()) || StringUtils.isBlank(form.getPasswordCheck())) {
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
        if (!form.getPasswordCheck().equals(form.getPassword())) {
            model.addAttribute("errors", "Passwords don't match");
            return "register";
        }

        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(form.getName(), form
                .getPassword(), Arrays.asList(new SimpleGrantedAuthority(Role.USER)));

        Team team = Team.builder()
                .company(form.getCompany())
                .country(form.getCountry())
                .name(form.getName())
                .password(encoder.encode(form.getPassword()))
                .role(Role.USER)
                .uuid(UUID.randomUUID())
                .build();

        teamRepository.save(team);
        context.setAuthentication(authentication);
        Path teamdir = mojServerProperties.getDirectories().getBaseDirectory()
                .resolve(mojServerProperties.getDirectories().getTeamDirectory())
                .resolve(authentication.getName());
        if (!Files.exists(teamdir)) {
            try {
                Files.createDirectory(teamdir);
            } catch (IOException e) {
                log.error("error creating teamdir", e);
            }
        }
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registrationForm(Model model) {
        model.addAttribute("form", new SignupForm());
        return "register";
    }

}
