package nl.moj.server;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import nl.moj.server.competition.Competition;

@Controller
public class TaskControlController {

	private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

	static ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

	@Autowired
	private SimpMessagingTemplate template;

	@Autowired
	private Competition competition;
	
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
		// final ScheduledFuture<?> handler = ex.scheduleAtFixedRate(() ->
		// sendRemainingTime(), 0, 1, TimeUnit.SECONDS);
		// ex.schedule(new Runnable() {
		// public void run() {
		// handler.cancel(false);
		// }
		// }, solutiontime, TimeUnit.SECONDS);
	}

	private void sendRemainingTime() {
		TaskTimeMessage taskTimeMessage = new TaskTimeMessage();
		taskTimeMessage.setRemainingTime(String.valueOf(competition.getRemainingTime()));
		template.convertAndSend("/queue/time", taskTimeMessage);
	}

	private void sendStartToTeams(String taskname) {
		template.convertAndSend("/queue/start", taskname);
	}

	@MessageMapping("/control/clearAssignment")
	@SendToUser("/control/queue/feedback")
	public void clearAssignment() {
		competition.clearCurrentAssignment();

	}

	@MessageMapping("/control/cloneAssignmentsRepo")
	@SendToUser("/queue/feedback")
	public String  cloneAssignmentsRepo() {
		return competition.cloneAssignmentsRepo();
	}

	@RequestMapping("/control")
	public String taskControl() {

		return "control";
	}

	public static class StartTaskMessage {
		private String taskName;

		public StartTaskMessage() {
		}

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
