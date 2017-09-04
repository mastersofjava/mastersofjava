package nl.moj.server;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import nl.moj.server.competition.TaskTimer;

@Controller
public class TaskControlController {

	private static final Logger log = LoggerFactory.getLogger(TaskControlController.class);

	static ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
	//Executors.newFixedThreadPool(2);
	@Autowired
	private TaskTimer timer;

	@Autowired
	private SimpMessagingTemplate template;

	@Autowired
	private Competition competition;

	@ModelAttribute(name = "assignmenNames")
	public Set<String> assignments() {
		return competition.getAssignmentNames();
	}

	@MessageMapping("/control/starttask")
	@SendToUser("/control/queue/feedback")
	public String startTask(StartTaskMessage message) {
		competition.setCurrentAssignment(message.getTaskName());
		timer.start(message.taskName);
		final ScheduledFuture<?> handler = ex.scheduleAtFixedRate(() -> sendTaskTime(), 0, 1, TimeUnit.SECONDS);
		ex.schedule(new Runnable() {
			public void run() {
				handler.cancel(false);
			}
		}, 10000, TimeUnit.MILLISECONDS);

		return "task started";
	}

	private void sendTaskTime() {
		TaskTimeMessage taskTimeMessage = new TaskTimeMessage();
		taskTimeMessage.setElapsedTime(String.valueOf(timer.getSeconds()));
		template.convertAndSendToUser("team1", "/queue/feedback", taskTimeMessage);
	}

	@MessageMapping("/control/getsplit")
	@SendToUser("/control/queue/feedback")
	public String getSplit() {
		int seconds = timer.getSeconds();
		return String.valueOf(seconds);
	}
	
	
	@MessageMapping("/control/clearAssignment")
	@SendToUser("/control/queue/feedback")
	public void clearAssignment() {
		competition.clearCurrentAssignment();
		
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
		private String elapsedTime;

		public String getElapsedTime() {
			return elapsedTime;
		}

		public void setElapsedTime(String elapsedTime) {
			this.elapsedTime = elapsedTime;
		}
	}

}
