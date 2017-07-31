package nl.moj.server;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

	@GetMapping("/")
	public String index(Model model) {
		List<String> files = new ArrayList<>();
		files.add("opgave");
		files.add("anderbestand");
		files.add("noganderbestand");
		model.addAttribute("files", files);
		return "index";
	}
	
	@GetMapping(value = "index.js")
    public String common(Model model) {
        model.addAttribute("code", "Thymeleaf rules!".hashCode());
		List<String> files = new ArrayList<>();
		files.add("opgave");
		files.add("anderbestand");
		files.add("noganderbestand");
		model.addAttribute("files", files);
        return "index.js";
}
}
