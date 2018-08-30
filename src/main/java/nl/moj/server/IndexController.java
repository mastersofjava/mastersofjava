package nl.moj.server;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import nl.moj.server.runtime.Competition;
import nl.moj.server.files.AssignmentFile;
import nl.moj.server.files.FileType;

@Controller
public class IndexController {

	private Competition competition;

	public IndexController(Competition competition) {
		super();
		this.competition = competition;
	}

	@GetMapping("/")
	public String index(Model model, @AuthenticationPrincipal Principal user) {
		if (competition.getCurrentAssignment() == null) {
			model.addAttribute("team", user.getName());
			return "index";
		}
		addModel(model, user);
		return "index";
	}

	private void addModel(Model model, Principal user) {
		List<AssignmentFile> files = new ArrayList<>();
		if (competition.getCurrentAssignment().isRunning() && !competition.getCurrentAssignment().isTeamFinished(user.getName())) {
			List<AssignmentFile> backupFiles = competition.getBackupFilesForTeam(user.getName());
			if (!backupFiles.isEmpty()) {
				files.addAll(backupFiles);
			} else {
				files.addAll(competition.getCurrentAssignment().getEditableFiles());
			}
		} else {
			files.addAll(competition.getCurrentAssignment().getEditableFiles());
		}
		files.addAll(competition.getCurrentAssignment().getReadOnlyJavaFiles());
		files.addAll(competition.getCurrentAssignment().getTaskFiles());
		files.sort(new Comparator<AssignmentFile>() {

			@Override
			public int compare(AssignmentFile arg0, AssignmentFile arg1) {
				if (arg0.getFileType().equals(FileType.TASK)) {
					return -10;
				}
				return 10;
			}
		});
		model.addAttribute("assignment", competition.getCurrentAssignment().getName());
		model.addAttribute("team", user.getName());
		model.addAttribute("timeLeft", competition.getRemainingTime());
		model.addAttribute("time", competition.getCurrentAssignment().getSolutionTime());
		model.addAttribute("testnames", competition.getCurrentAssignment().getTestNames());
		model.addAttribute("files", files);
		model.addAttribute("running", competition.getCurrentAssignment().isRunning());
		model.addAttribute("finished", competition.getCurrentAssignment().isTeamFinished(user.getName()));
		model.addAttribute("submittime", competition.getCurrentAssignment().getTeamSubmitTime(user.getName()));
		model.addAttribute("finalscore", competition.getCurrentAssignment().getTeamFinalScore(user.getName()));
	}

}
