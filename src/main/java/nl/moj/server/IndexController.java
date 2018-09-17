package nl.moj.server;

import lombok.AllArgsConstructor;
import nl.moj.server.rankings.service.RankingsService;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.AssignmentState;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@AllArgsConstructor
public class IndexController {

	private static final Logger log = LoggerFactory.getLogger(IndexController.class);
	private CompetitionRuntime competition;
	private TeamRepository teamRepository;
	private RankingsService rankingsService;


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

		List<AssignmentFile> files = new ArrayList<>();
		if (state.isRunning() && !state.isTeamFinished(team)) {
			files.addAll(competition.getTeamAssignmentFiles(team));
		} else {
			files.addAll(state.getAssignmentFiles());
		}
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
		model.addAttribute("testnames", state.getTestNames());
		model.addAttribute("files", files);
		model.addAttribute("running", state.isRunning());
		model.addAttribute("finished", state.isTeamFinished(team));
		model.addAttribute("submittime", state.getTeamStatus(team).getSubmitTime());
		model.addAttribute("finalscore", state.getTeamStatus(team).getScore());
	}

}
