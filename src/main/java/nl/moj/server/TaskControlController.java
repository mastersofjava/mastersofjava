package nl.moj.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.assignment.service.AssignmentServiceException;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;

@Controller
@RequiredArgsConstructor
public class TaskControlController {

    private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

    private final MojServerProperties mojServerProperties;

    private final CompetitionRuntime competition;

    private final AssignmentService assignmentService;

    private final AssignmentRepository assignmentRepository;

    private final CompetitionRepository competitionRepository;

    private final TeamRepository teamRepository;
    
    private final PasswordEncoder encoder;
    
    @ModelAttribute(name = "assignments")
    public List<ImmutablePair<String, Long>> assignments() {
        return competition.getAssignmentInfo();
    }

    @MessageMapping("/control/starttask")
    public void startTask(TaskMessage message) {
        competition.startAssignment(message.getTaskName());
    }

    @MessageMapping("/control/stoptask")
    public void stopTask() {
        competition.stopCurrentAssignment();
    }

    @MessageMapping("/control/clearCurrentAssignment")
    @SendToUser("/queue/controlfeedback")
    public void clearAssignment() {
    }

    @MessageMapping("/control/scanAssignments")
    @SendToUser("/queue/controlfeedback")
    public String cloneAssignmentsRepo() {
        try {
            assignmentService.updateAssignments(mojServerProperties.getAssignmentRepo());
            UUID competitionUuid = mojServerProperties.getCompetition().getUuid();
            Competition c = competitionRepository.findByUuid(competitionUuid);
            if (c == null) {
                c = new Competition();
                c.setUuid(competitionUuid);
            }
            c.setName("Masters of Java 2018");

            // wipe assignments
            c.setAssignments(new ArrayList<>());
            c = competitionRepository.save(c);

            // re-add updated assignments
            c.setAssignments(assignmentRepository.findAll()
                    .stream()
                    .map(createOrderedAssignments(c))
                    .collect(Collectors.toList()));
            c = competitionRepository.save(c);

            competition.startCompetition(c);

            return "Assignments scanned, reload to show them.";
        } catch (AssignmentServiceException ase) {
            log.error("Scanning assignments failed.", ase);
            return ase.getMessage();
        }
    }

    private Function<Assignment, OrderedAssignment> createOrderedAssignments(Competition c) {
        AtomicInteger count = new AtomicInteger(0);
        return a -> {
            OrderedAssignment oa = new OrderedAssignment();
            oa.setAssignment(a);
            oa.setCompetition(c);
            oa.setUuid(UUID.randomUUID());
            oa.setOrder(count.getAndIncrement());
            return oa;
        };
    }

    @GetMapping("/control")
    public String taskControl(Model model) {
        if (competition.getCurrentAssignment() != null) {
            ActiveAssignment state = competition.getActiveAssignment();
            model.addAttribute("timeLeft", state.getTimeRemaining());
            model.addAttribute("time", state.getAssignmentDescriptor().getDuration().toSeconds());
            model.addAttribute("running", state.isRunning());
            model.addAttribute("currentAssignment", state.getAssignmentDescriptor().getName());
        } else {
            model.addAttribute("timeLeft", 0);
            model.addAttribute("time", 0);
            model.addAttribute("running", false);
            model.addAttribute("currentAssignment", "-");
        }
        
        model.addAttribute("teams", teamRepository.findAll());
		if (!model.containsAttribute("newPasswordRequest")) {
			model.addAttribute("newPasswordRequest", new NewPasswordRequest());
		} 
        return "control";
    }

	@PostMapping("/control/resetPassword")
	public String resetPassword(RedirectAttributes redirectAttributes,
			@ModelAttribute("newPasswordRequest") NewPasswordRequest passwordChangeRequest) {

		String errorMessage = null;
		
		if (passwordChangeRequest.teamUuid.equals("0")) {
			errorMessage = "No team selected";
		} else {
			Team team = teamRepository.findByUuid(UUID.fromString(passwordChangeRequest.teamUuid));
			if (passwordChangeRequest.newPassword.isBlank()) {
				errorMessage = "New password can't be empty";
			} else if (!passwordChangeRequest.newPassword.equals(passwordChangeRequest.newPasswordCheck)) {
				errorMessage = "Password and confirmaton did not match";
			} else {
				team.setPassword(encoder.encode(passwordChangeRequest.newPassword));
				teamRepository.save(team);
				redirectAttributes.addFlashAttribute("success", "Successfully changed password");
				return "redirect:/control";
			}
		}
	
		passwordChangeRequest.clearPasswords();
		redirectAttributes.addFlashAttribute("newPasswordRequest", passwordChangeRequest);
		redirectAttributes.addFlashAttribute("error", errorMessage);

		return "redirect:/control";
	}

	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class TaskMessage {
		private String taskName;
    }

    @Data
    public static class NewPasswordRequest{
    	private String teamUuid;
        private String newPassword;
        private String newPasswordCheck;
        
        public void clearPasswords() {
        	newPassword=null;
        	newPasswordCheck = null;
        }
    }
}
