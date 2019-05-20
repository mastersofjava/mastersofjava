package nl.moj.server;

import lombok.AllArgsConstructor;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.runtime.model.AssignmentFileType;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.submit.model.SubmitAttempt;
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
	private AssignmentStatusRepository assignmentStatusRepository;

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
		ActiveAssignment state = competition.getActiveAssignment();
		Team team = teamRepository.findByName(user.getName());
		AssignmentStatus as = assignmentStatusRepository.findByAssignmentAndCompetitionSessionAndTeam(state.getAssignment(),state.getCompetitionSession(),team);
		AssignmentStatusHelper ash = new AssignmentStatusHelper(as,state.getAssignmentDescriptor());

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
		model.addAttribute("finished", ash.isCompleted());
		model.addAttribute("submittime", ash.getSubmitTime());
		model.addAttribute("finalscore", ash.getScore());
		model.addAttribute("maxSubmits", ash.getMaximumSubmits() );
		model.addAttribute("submits", ash.getRemainingSubmits());

	}


	class AssignmentStatusHelper {

		private AssignmentStatus assignmentStatus;
		private AssignmentDescriptor assignmentDescriptor;

		public AssignmentStatusHelper(AssignmentStatus assignmentStatus, AssignmentDescriptor assignmentDescriptor) {
			this.assignmentStatus = assignmentStatus;
			this.assignmentDescriptor = assignmentDescriptor;
		}

		public boolean isCompleted() {
			return assignmentStatus.getSubmitAttempts().stream().anyMatch(SubmitAttempt::isSuccess) ||
					assignmentStatus.getSubmitAttempts().size() >= (assignmentDescriptor.getScoringRules().getMaximumResubmits() + 1);
		}

		public long getSubmitTime() {
			return assignmentStatus.getAssignmentResult().getInitialScore();
		}

		public int getMaximumSubmits() {
			return assignmentDescriptor.getScoringRules().getMaximumResubmits() + 1;
		}

		public int getRemainingSubmits() {
			return getMaximumSubmits() - assignmentStatus.getSubmitAttempts().size();
		}

		public long getScore() {
			return assignmentStatus.getAssignmentResult().getFinalScore();
		}

	}
}
