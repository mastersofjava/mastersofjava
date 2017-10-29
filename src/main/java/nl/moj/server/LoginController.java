package nl.moj.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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

import nl.moj.server.model.Team;
import nl.moj.server.persistence.TeamMapper;

@Controller
public class LoginController {
	
	private static final Logger log = LoggerFactory.getLogger(LoginController.class);
	
	private TeamMapper teamMapper;
	
	private PasswordEncoder encoder;
	
	private DirectoriesConfiguration directories;
	
    public LoginController(TeamMapper teamMapper, PasswordEncoder encoder, DirectoriesConfiguration directories) {
		super();
		this.teamMapper = teamMapper;
		this.encoder = encoder;
		this.directories = directories;
	}

	@GetMapping("/login")
    public String loginForm(Model model) {
        return "login";
    }

    @PostMapping("/register")
    public String registerSubmit(Model model, @ModelAttribute Team team) {
    	if(team.getName() == ""|| team.getPassword() == ""|| team.getCpassword() == ""){
    		model.addAttribute("errors", "Not all fields are filled in");
    		return "register";
    	}
    	if(teamMapper.findByName(team.getName()) != null){
    		model.addAttribute("errors", "Name already in use");
    		return "register";
    	}
    	if(!team.getCpassword().equals(team.getPassword())){
    		model.addAttribute("errors", "Passwords don't match");
    		return "register";
    	}
    	team.setRole("ROLE_USER");
    	team.setPassword(encoder.encode(team.getPassword()));
    	teamMapper.insertTeam(team);
    	SecurityContext context = SecurityContextHolder.getContext();
    	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(team.getName(), team.getPassword(), Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
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
