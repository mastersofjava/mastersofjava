package nl.moj.server.runtime;


import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class TaskTimer {

	private StopWatch timer;
	
	public void start() {
		timer = StopWatch.createStarted();
	}
	
	public int getSeconds() {
		return (int) timer.getTime(TimeUnit.SECONDS);
	}
	
	public String stop() {
		timer.stop();
		return timer.toString();
	}
}
