package nl.moj.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import nl.moj.server.competition.Competition;
import nl.moj.server.model.Result;
import nl.moj.server.persistence.ResultMapper;

@Controller
public class TaskControlController {

	private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

	static ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

	@Autowired
	private SimpMessagingTemplate template;

	@Autowired
	private Competition competition;

	@Autowired
	private ResultMapper resultMapper;

	@ModelAttribute(name = "assignmenNames")
	public Set<String> assignments() {
		return competition.getAssignmentNames();
	}

	@MessageMapping("/control/starttask")
	public void startTask(StartTaskMessage message) {
		competition.setCurrentAssignment(message.getTaskName());
		Integer solutiontime = competition.getCurrentAssignment().getSolutionTime();
		competition.startCurrentAssignment();
		sendStartToTeams(message.taskName);
		final ScheduledFuture<?> handler = ex.scheduleAtFixedRate(() -> sendRemainingTime(), 0, 1, TimeUnit.SECONDS);
		ex.schedule(new Runnable() {
			public void run() {
				sendStopToTeams(message.taskName);
				handler.cancel(false);
			}
		}, solutiontime, TimeUnit.SECONDS);
	}

	@MessageMapping("/control/stoptask")
	public void stopTask(StartTaskMessage message) {
		sendStopToTeams(message.taskName);
	}

	private void sendRemainingTime() {
		TaskTimeMessage taskTimeMessage = new TaskTimeMessage();
		taskTimeMessage.setRemainingTime(String.valueOf(competition.getRemainingTime()));
		template.convertAndSend("/queue/time", taskTimeMessage);
	}

	private void sendStartToTeams(String taskname) {
		template.convertAndSend("/queue/start", taskname);
	}

	private void sendStopToTeams(String taskname) {
		template.convertAndSend("/queue/stop", taskname);
	}

	@MessageMapping("/control/clearAssignment")
	@SendToUser("/control/queue/feedback")
	public void clearAssignment() {
		competition.clearCurrentAssignment();

	}

	@MessageMapping("/control/cloneAssignmentsRepo")
	@SendToUser("/queue/feedback")
	public String cloneAssignmentsRepo() {
		return competition.cloneAssignmentsRepo();
	}

	@GetMapping("/control")
	public String taskControl() {

		return "control";
	}

	@GetMapping(value = "getResultsAsCSV", produces = "text/csv")
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

	enum ResultHeaders {
		TEAM, ASSIGNMENT, SCORE, PENALTY, CREDIT
	}

	public static class StartTaskMessage {
		private String taskName;

		public String getTaskName() {
			return taskName;
		}

		public void setTaskName(String taskName) {
			this.taskName = taskName;
		}

	}

	public static class TaskTimeMessage {
		private String remainingTime;

		public String getRemainingTime() {
			return remainingTime;
		}

		public void setRemainingTime(String remainingTime) {
			this.remainingTime = remainingTime;
		}
	}

}