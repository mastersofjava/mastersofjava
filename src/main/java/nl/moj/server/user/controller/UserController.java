package nl.moj.server.user.controller;

import javax.annotation.security.RolesAllowed;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.authorization.Role;
import nl.moj.server.user.service.UserService;

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
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            log.error("Delete of user {} failed.", name, e);
        }
        return ResponseEntity.badRequest().build();
    }
}
