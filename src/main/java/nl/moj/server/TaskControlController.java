package nl.moj.server;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.assignment.service.AssignmentServiceException;
import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.OrderedAssignment;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.config.properties.MojServerProperties;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.runtime.CompetitionRuntime;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.Result;
import nl.moj.server.teams.model.Team;
import nl.moj.server.teams.repository.TeamRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nl.moj.server.teams.model.Role.ROLE_USER;

@Controller
@RequiredArgsConstructor
public class TaskControlController {

	private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

	private final MojServerProperties mojServerProperties;

	private final CompetitionRuntime competition;

	private final ResultRepository resultRepository;

	private final TeamRepository teamRepository;

	private final MessageService feedbackMessageController;

	private final AssignmentService assignmentService;

	private final AssignmentRepository assignmentRepository;

	private final CompetitionRepository competitionRepository;

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
			c.setAssignments(assignmentRepository.findAll().stream().map(createOrderedAssignments(c)).collect(Collectors.toList()));
			c = competitionRepository.save(c);

			competition.startCompetition(c);

			return "Assignments scanned, reload to show them.";
		} catch( AssignmentServiceException ase ) {
			log.error("Scanning assignments failed.",ase);
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
		return "control";
	}

	@GetMapping(value = "/getresults", produces = "text/csv")
	@ResponseBody
	public void getResultsAsCSV(HttpServletResponse response) {
		response.setHeader("Content-Disposition", "attachment; filename=\"results.csv\"");
		try (CSVPrinter printer = new CSVPrinter(response.getWriter(),
				CSVFormat.DEFAULT.withHeader(ResultHeaders.class))) {
			List<Result> allResults = resultRepository.findAllByOrderByTeamNameAsc();
			allResults.forEach((r) -> {
				try {
					printer.printRecord(r.getTeam().getUuid(), r.getTeam().getName(), r.getAssignment(), r.getScore(), r.getPenalty(), r.getCredit());
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			});
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	@PostMapping("/upload")
	public String singleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		if (file.isEmpty()) {
			log.error("file empty");
			return "control";
		}
		try {
			Reader in = new InputStreamReader(file.getInputStream());
			if (file.getName().equalsIgnoreCase("results.csv")) {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(ResultHeaders.class)
						.withFirstRecordAsHeader().parse(in);
				for (CSVRecord record : records) {
					resultRepository.save(createResultFromRecord(record));
				}
			} else if (file.getName().equalsIgnoreCase("results-update.csv")) {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(ResultHeaders.class)
						.withFirstRecordAsHeader().parse(in);
				for (CSVRecord record : records) {
                    resultRepository.save(createResultFromRecord(record));
				}
			} else {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(TeamHeaders.class).withFirstRecordAsHeader()
						.parse(in);
				for (CSVRecord r : records) {
					Team t = Team.builder()
							.uuid(UUID.fromString(r.get(TeamHeaders.ID)))
							.name(r.get(TeamHeaders.NAME))
							.company(r.get(TeamHeaders.COMPANY))
							.country(r.get(TeamHeaders.COUNTRY))
							.build();
					teamRepository.save(t);
				}
			}

		} catch (IllegalStateException | IOException e) {
			log.error(e.getMessage(), e);
		}
		return "control";
	}

	private Result createResultFromRecord( CSVRecord record ) {
		Result r = new Result();
		r.setTeam(teamRepository.findByUuid(UUID.fromString(record.get(ResultHeaders.TEAM_ID))));
		r.setCompetitionSession(competition.getCompetitionSession());
		r.setAssignment(assignmentRepository.findByName(record.get(ResultHeaders.ASSIGNMENT)));
		r.setCredit(Integer.valueOf(record.get(ResultHeaders.CREDIT)));
		r.setPenalty(Integer.valueOf(record.get(ResultHeaders.PENALTY)));
		r.setScore(Integer.valueOf(record.get(ResultHeaders.SCORE)));
		return r;
	}

	@GetMapping(value = "/getteams", produces = "text/csv")
	@ResponseBody
	public void getTeamsAsCSV(HttpServletResponse response) {
		response.setHeader("Content-Disposition", "attachment; filename=\"teams.csv\"");
		try (CSVPrinter printer = new CSVPrinter(response.getWriter(),
				CSVFormat.DEFAULT.withHeader(TeamHeaders.class))) {
			List<Team> allTeams = teamRepository.findAllByRole(ROLE_USER);
			allTeams.forEach((t) -> {
				try {
					printer.printRecord(t.getUuid(),t.getName(), t.getCompany(), t.getCountry());
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			});
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	private enum ResultHeaders {
		TEAM_ID, TEAM, ASSIGNMENT, SCORE, PENALTY, CREDIT
	}

	private enum TeamHeaders {
		ID, NAME, COMPANY, COUNTRY
	}

	public static class TaskMessage {
		private String taskName;

		public TaskMessage() {
		}

		public TaskMessage(String taskName) {
			this.taskName = taskName;
		}

		public String getTaskName() {
			return taskName;
		}

		public void setTaskName(String taskName) {
			this.taskName = taskName;
		}
	}
}
