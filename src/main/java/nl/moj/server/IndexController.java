package nl.moj.server;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import nl.moj.server.files.AssignmentFile;

@Controller
public class IndexController {

	private AssignmentService assignmentService;
	
	
	public IndexController(AssignmentService assignmentService) {
		super();
		this.assignmentService = assignmentService;
	}

	@GetMapping("/")
	public String index(Model model) {
		List<AssignmentFile> files = assignmentService.getJavaFiles();
		model.addAttribute("files", files);
		return "index";
	}

	@GetMapping(value = "index.js")
	public String common(Model model) {
		List<AssignmentFile> files = assignmentService.getJavaFiles();
		model.addAttribute("editables", assignmentService.getEditableFileNames());
		model.addAttribute("files", files);
		return "index.js";
	}
}
