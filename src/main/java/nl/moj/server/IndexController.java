package nl.moj.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import nl.moj.server.competition.Competition;
import nl.moj.server.files.AssignmentFile;

@Controller
public class IndexController {

	@Autowired
	private Competition competition;

	@GetMapping("/")
	public String index(Model model) {
		if (competition.getCurrentAssignment() == null) {
			return "index";
		}
		List<AssignmentFile> files = competition.getCurrentAssignment().getJavaFiles();
		files.addAll(competition.getCurrentAssignment().getTaskFiles());
		model.addAttribute("files", files);
		return "index";
	}

	@GetMapping(value = "index.js")
	public String common(Model model) {
		if (competition.getCurrentAssignment() == null) {
			return "index";
		}
		List<AssignmentFile> files = competition.getCurrentAssignment().getJavaFiles();
		files.addAll(competition.getCurrentAssignment().getTaskFiles());
		model.addAttribute("files", files);
		return "index.js";
	}
}
