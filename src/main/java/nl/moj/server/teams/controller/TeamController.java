package nl.moj.server.teams.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Slf4j
@Controller
@AllArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;

    @PostMapping("/team")
    public String createTeam(Principal principal, @ModelAttribute("teamForm") TeamForm form) {
        User user = userService.createOrUpdate(principal);
        Team team = teamService.createOrUpdate(form.getName(),form.getCompany(), form.getCountry());
        if( user.getTeam() == null ) {
            user = userService.addUserToTeam(user, team);
            log.info("Registered team {} with uuid {} for user {}", team.getName(), team.getUuid(), user.getName());
        }
        return "redirect:/play";
    }
}
