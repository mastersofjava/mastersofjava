package nl.moj.server.timed;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.tascalate.concurrent.CompletableTask;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Monitors a {@link CompletableFuture} and reports the time consumed to process
 * the {@link CompletableFuture}, the initiating and executing thread and a
 * description of the method executed.
 * 
 * @author Andreas Kluth <mail@andreaskluth.net>
 */
@Aspect
@Component
public class AsyncTimedAspect {

	private static final Logger log = LoggerFactory.getLogger(AsyncTimedAspect.class);

	@Pointcut("@annotation(nl.moj.server.timed.AsyncTimed)")
	public void asyncTimedAnnotationPointcut() {
	}

	@Around("asyncTimedAnnotationPointcut()")
	public Object methodsAnnotatedWithAsyncTime(final ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
	}

	private Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
		final Object result = joinPoint.proceed();
	//	if (!isCompletableFuture(result)) {
	//		return result;
	//	}

		final String description = joinPoint.toString();
		final StopWatch watch = new StopWatch();
		watch.start();

		String initiatingThread = Thread.currentThread().getName();
		return ((CompletableFuture<?>) result).thenApply(__ -> {
			String executingThread = Thread.currentThread().getName();
			watch.stop();
			log.info("Timed: {} ; {} ; Initiating Thread '{}' Executing Thread '{}'.", description, watch.toString(),
					initiatingThread, executingThread);
			System.out.println("Timed: " + description + " ; " + watch.toString());
			return __;
		});
	}

	private boolean isCompletableFuture(Object result) {
		return CompletableTask.class.isAssignableFrom(result.getClass());
	}
}