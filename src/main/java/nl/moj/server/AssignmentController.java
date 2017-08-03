package nl.moj.server;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.ScopedProxyMode;

@RestController
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssignmentController {

	private AssignmentService assignmentService;
	
	public AssignmentController(AssignmentService assignmentService) {
		super();
		this.assignmentService = assignmentService;
	}

	@RequestMapping("/files")
	public List<JavaFile> getFiles() {
		return assignmentService.getAssignmentFiles();
	}

	@RequestMapping("/file/{filename}")
	public List<JavaFile> getFile(String filename) {
		return assignmentService.getAssignmentFiles();
	}

}
