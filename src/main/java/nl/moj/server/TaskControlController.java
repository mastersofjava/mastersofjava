package nl.moj.server;

import lombok.AllArgsConstructor;
import nl.moj.server.AssignmentRepoConfiguration.Repo;
import nl.moj.server.competition.Competition;
import nl.moj.server.model.Result;
import nl.moj.server.repository.ResultRepository;
import nl.moj.server.model.Team;
import nl.moj.server.repository.TeamRepository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
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
import java.util.List;

@Controller
@AllArgsConstructor
public class TaskControlController {

	private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

	private final Competition competition;

	private final ResultRepository resultRepository;

	private final TeamRepository teamRepository;

	private final AssignmentRepoConfiguration repos;

	private final FeedbackMessageController feedbackMessageController;

	@ModelAttribute(name = "assignments")
	public List<ImmutablePair<String, Integer>> assignments() {
		return competition.getAssignmentInfo();
	}

	@ModelAttribute(name = "repos")
	public List<Repo> repos() {
		return repos.getRepos();
	}

	@MessageMapping("/control/starttask")
	public void startTask(TaskMessage message) {
		feedbackMessageController.sendStartToTeams(message.taskName);
		competition.startAssignment(message.getTaskName());
	}

	@MessageMapping("/control/stoptask")
	public void stopTask(TaskMessage message) {
		competition.stopCurrentAssignment();
		feedbackMessageController.sendStopToTeams(message.taskName);
	}

	@MessageMapping("/control/clearCurrentAssignment")
	@SendToUser("/queue/controlfeedback")
	public void clearAssignment() {
		competition.clearCurrentAssignment();
	}

	@MessageMapping("/control/cloneAssignmentsRepo")
	@SendToUser("/queue/controlfeedback")
	public String cloneAssignmentsRepo(Message<String> repoName) {
		return competition.cloneAssignmentsRepo(repoName.getPayload());
	}

	@GetMapping("/control")
	public String taskControl(Model model) {
		if (competition.getCurrentAssignment() != null) {
			model.addAttribute("timeLeft", competition.getRemainingTime());
			model.addAttribute("time", competition.getCurrentAssignment().getSolutionTime());
			model.addAttribute("running", competition.getCurrentAssignment().isRunning());
			model.addAttribute("currentAssignment", competition.getCurrentAssignment().getName());
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
			List<Result> allResults = resultRepository.findAllOrderByTeam();
			allResults.forEach((r) -> {
				try {
					printer.printRecord(r.getTeam(), r.getAssignment(), r.getScore(), r.getPenalty(), r.getCredit());
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
					Result r = new Result(teamRepository.findByName(record.get(ResultHeaders.TEAM)), record.get(ResultHeaders.ASSIGNMENT),
							Integer.valueOf(record.get(ResultHeaders.SCORE)), Integer.valueOf(record.get(ResultHeaders.PENALTY)),
							Integer.valueOf(record.get(ResultHeaders.CREDIT)));
					resultRepository.save(r);
				}
			} else if (file.getName().equalsIgnoreCase("results-update.csv")) {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(ResultHeaders.class)
						.withFirstRecordAsHeader().parse(in);
				for (CSVRecord record : records) {
					Result r = new Result(teamRepository.findByName(record.get(ResultHeaders.TEAM)), record.get(ResultHeaders.ASSIGNMENT),
							Integer.valueOf(record.get(ResultHeaders.SCORE)), Integer.valueOf(record.get(ResultHeaders.PENALTY)),
							Integer.valueOf(record.get(ResultHeaders.CREDIT)));
                    resultRepository.save(r);
				}
			} else {
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(TeamHeaders.class).withFirstRecordAsHeader()
						.parse(in);
				for (CSVRecord r : records) {
					Team t = new Team(r.get(TeamHeaders.NAME), "ROLE_USER", r.get(TeamHeaders.COUNTRY),
							r.get(TeamHeaders.COMPANY));
					teamRepository.save(t);
				}

			}

		} catch (IllegalStateException | IOException e) {
			log.error(e.getMessage(), e);
		}
		return "control";
	}

	@GetMapping(value = "/getteams", produces = "text/csv")
	@ResponseBody
	public void getTeamsAsCSV(HttpServletResponse response) {
		response.setHeader("Content-Disposition", "attachment; filename=\"teams.csv\"");
		try (CSVPrinter printer = new CSVPrinter(response.getWriter(),
				CSVFormat.DEFAULT.withHeader(TeamHeaders.class))) {
			List<Team> allTeams = teamRepository.findAllByRole("ROLE_USER");
			allTeams.forEach((t) -> {
				try {
					printer.printRecord(t.getName(), t.getCompany(), t.getCountry());
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			});
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	private enum ResultHeaders {
		TEAM, ASSIGNMENT, SCORE, PENALTY, CREDIT
	}

	private enum TeamHeaders {
		NAME, COMPANY, COUNTRY
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
