package nl.moj.server;

import lombok.AllArgsConstructor;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@AllArgsConstructor
public class IndexController {

	private CompetitionRuntime competition;
	private TeamRepository teamRepository;

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
		AssignmentState state = competition.getAssignmentState();
		Team team = teamRepository.findByName(user.getName());

		List<AssignmentFile> files = competition.getTeamAssignmentFiles(team);
		
		// TODO ugly
		files.sort((arg0, arg1) -> {
			if (arg0.getFileType().equals(AssignmentFileType.TASK.TASK)) {
				return -10;
			}
			return 10;
		});
		model.addAttribute("assignment", state.getAssignmentDescriptor().getName());
		model.addAttribute("team", user.getName());
		model.addAttribute("timeLeft", state.getTimeRemaining());
		model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
		model.addAttribute("tests", state.getTestFiles());
		model.addAttribute("files", files);
		model.addAttribute("running", state.isRunning());
		model.addAttribute("finished", state.getTeamStatus(team).isCompleted());
		model.addAttribute("submittime", state.getTeamStatus(team).getSubmitTime());
		model.addAttribute("finalscore", state.getTeamStatus(team).getScore());
		model.addAttribute("maxSubmits", state.getAssignmentDescriptor().getScoringRules().getMaximumResubmits() + 1 );
		model.addAttribute("submits", state.getRemainingSubmits(team));
	}

}
