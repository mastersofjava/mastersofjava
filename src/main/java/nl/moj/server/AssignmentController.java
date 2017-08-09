package nl.moj.server;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nl.moj.server.files.AssignmentFile;

@RestController
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssignmentController {

	private AssignmentService assignmentService;
	
	public AssignmentController(AssignmentService assignmentService) {
		super();
		this.assignmentService = assignmentService;
	}

	@RequestMapping("/files")
	public List<AssignmentFile> getFiles() {
		return assignmentService.getJavaFiles();
	}

	@RequestMapping("/file/{filename}")
	public List<AssignmentFile> getFile(String filename) {
		return assignmentService.getJavaFiles();
	}

}
