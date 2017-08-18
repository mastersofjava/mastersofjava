package nl.moj.server.competition;


import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

@Service
public class TaskTimer {

	private String taskname;
	
	private Stopwatch timer;
	
	public void start(String taskname) {
		timer = Stopwatch.createStarted();
		this.taskname = taskname;
	}
	
	public int getSeconds() {
		return (int) timer.elapsed(TimeUnit.SECONDS);
	}
	
	public String stop() {
		timer.stop();
		return timer.toString();
	}
}
