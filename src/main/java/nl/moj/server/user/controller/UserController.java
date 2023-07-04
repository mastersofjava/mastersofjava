package nl.moj.server.user.controller;

import javax.annotation.security.RolesAllowed;
import java.security.Principal;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.authorization.Role;
import nl.moj.server.teams.controller.TeamForm;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.user.model.User;
import nl.moj.server.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @DeleteMapping("/users/{name}")
    @RolesAllowed(Role.ADMIN)
    public ResponseEntity<Void> deleteByName(@PathVariable("name") String name) {
        try {
            if (userService.deleteUser(name)) {
                ResponseEntity.noContent();
            }
        } catch (Exception e) {
            log.error("Delete of user {} failed.", name, e);
        }
        return ResponseEntity.badRequest().build();
    }
}
