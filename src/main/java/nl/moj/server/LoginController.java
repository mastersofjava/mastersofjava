package nl.moj.server;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
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
	@Autowired
	private TeamMapper teamMapper;
	
	@Autowired
	private PasswordEncoder encoder;
	
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
    		model.addAttribute("errors", "Name allready in use");
    		return "register";
    	}
    	if(!team.getCpassword().equals(team.getPassword())){
    		model.addAttribute("errors", "Passwords don't match");
    		return "register";
    	}
    	
    	teamMapper.addTeam(team.getName(), encoder.encode(team.getPassword()),"ROLE_USER");
    	SecurityContext context = SecurityContextHolder.getContext();
    	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(team.getName(), team.getPassword(), Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
    	context.setAuthentication(authentication);
    	
    	return "redirect:/";
    }
    
    @GetMapping("/register")
    public String registrationForm(Model model) {
        model.addAttribute("team", new Team());
        return "register";
    }

}
