package nl.moj.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nl.moj.server.AssignmentRepoConfiguration.Repo;
import nl.moj.server.competition.Competition;
import nl.moj.server.model.Result;
import nl.moj.server.persistence.ResultMapper;

@Controller
public class TaskControlController {

	private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

	private Competition competition;

	private ResultMapper resultMapper;

	private AssignmentRepoConfiguration repos;

	private FeedbackMessageController feedbackMessageController;
	private SoundService soundService;
	
	public TaskControlController(Competition competition, ResultMapper resultMapper, AssignmentRepoConfiguration repos,
			FeedbackMessageController feedbackMessageController, SoundService soundService) {
		super();
		this.competition = competition;
		this.resultMapper = resultMapper;
		this.repos = repos;
		this.feedbackMessageController = feedbackMessageController;
		this.soundService = soundService;
	}

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
		soundService.playTicTac1Sound();
		
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
		} else {
			model.addAttribute("timeLeft", 0);
			model.addAttribute("time", 0);
			model.addAttribute("running", false);
		}
		return "control";
	}

	@GetMapping(value = "/getresults", produces = "text/csv")
	@ResponseBody
	public void getResultsAsCSV(HttpServletResponse response) {
		response.setHeader("Content-Disposition", "attachment; filename=\"results.csv\"");
		try (CSVPrinter printer = new CSVPrinter(response.getWriter(),
				CSVFormat.DEFAULT.withHeader(ResultHeaders.class))) {
			List<Result> allResults = resultMapper.getAllResults();
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
		}
		try {
			Reader in = new InputStreamReader(file.getInputStream());
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(ResultHeaders.class).withFirstRecordAsHeader()
					.parse(in);
			for (CSVRecord record : records) {
				Result r = new Result(record.get(0), record.get(1), Integer.valueOf(record.get(2)),
						Integer.valueOf(record.get(3)), Integer.valueOf(record.get(4)));
				resultMapper.insertResult(r);
			}
		} catch (IllegalStateException | IOException e) {
			log.error(e.getMessage(), e);
		}
		return "control";
	}

	private enum ResultHeaders {
		TEAM, ASSIGNMENT, SCORE, PENALTY, CREDIT
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
