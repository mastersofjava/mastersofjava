package nl.moj.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${moj.server.teamDirectory}")
	private String teamDirectory;

	@Value("${moj.server.basedir}")
	private String basedir;

	
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
    	
    	teamMapper.addTeam(team.getName(), encoder.encode(team.getPassword()),"ROLE_USER");
    	//for(String assignment : competition.getAssignmentNames()){
    	//	resultMapper.insertEmptyResult(team.getName(), assignment);
    	//}
    	
    	SecurityContext context = SecurityContextHolder.getContext();
    	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(team.getName(), team.getPassword(), Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
    	context.setAuthentication(authentication);
    	Path teamdir = Paths.get(basedir, teamDirectory, authentication.getName());
		if (!Files.exists(teamdir)) {
			try {
				Files.createDirectory(teamdir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
