package nl.moj.server.runtime;


import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

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
