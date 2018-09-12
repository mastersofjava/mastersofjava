package nl.moj.server;

import lombok.RequiredArgsConstructor;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static nl.moj.server.model.Role.ROLE_USER;

@Controller
@RequiredArgsConstructor
public class LoginController {
	
	private static final Logger log = LoggerFactory.getLogger(LoginController.class);
	
	private final TeamRepository teamRepository;
	
	private final PasswordEncoder encoder;
	
	private final DirectoriesConfiguration directories;

	@GetMapping("/login")
    public String loginForm(Model model) {
        return "login";
    }

    @PostMapping("/register")
    public String registerSubmit(Model model, @ModelAttribute Team team) {
    	if (team.getName() == ""|| team.getPassword() == ""|| team.getCpassword() == "") {
    		model.addAttribute("errors", "Not all fields are filled in");
    		return "register";
    	}
    	if (teamRepository.findByName(team.getName()) != null) {
    		model.addAttribute("errors", "Name already in use");
    		return "register";
    	}
    	if (!team.getCpassword().equals(team.getPassword())) {
    		model.addAttribute("errors", "Passwords don't match");
    		return "register";
    	}
    	team.setRole(ROLE_USER);
    	team.setPassword(encoder.encode(team.getPassword()));
        teamRepository.save(team);
    	SecurityContext context = SecurityContextHolder.getContext();
    	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(team.getName(), team.getPassword(), Arrays.asList(new SimpleGrantedAuthority(ROLE_USER.toString())));
    	context.setAuthentication(authentication);
    	Path teamdir = Paths.get(directories.getBaseDirectory(), directories.getTeamDirectory(), authentication.getName());
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
        model.addAttribute("team", new Team());
        return "register";
    }
    
}
