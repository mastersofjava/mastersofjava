package nl.moj.server.competition;


import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

public class TaskTimer {

	private Stopwatch timer;
	
	public void start() {
		timer = Stopwatch.createStarted();
	}
	
	public int getSeconds() {
		return (int) timer.elapsed(TimeUnit.SECONDS);
	}
	
	public String stop() {
		timer.stop();
		return timer.toString();
	}
}
