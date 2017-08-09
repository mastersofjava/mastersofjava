package nl.moj.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.imageio.stream.FileImageInputStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

	private AssignmentService assignmentService;
	
	
	public IndexController(AssignmentService assignmentService) {
		super();
		this.assignmentService = assignmentService;
	}

	@GetMapping("/")
	public String index(Model model) {
		List<JavaFile> files = assignmentService.getAssignmentFiles();
		model.addAttribute("files", files);
		return "index";
	}

	@GetMapping(value = "index.js")
	public String common(Model model) {
		List<JavaFile> files = assignmentService.getAssignmentFiles();
		model.addAttribute("editables", assignmentService.getEditableFileNames());
		model.addAttribute("files", files);
		return "index.js";
	}
}
