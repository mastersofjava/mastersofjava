package nl.moj.server.timed;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

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

  private static final Log LOG = LogFactory.getLog(AsyncTimed.class);

  @Pointcut("@annotation(nl.moj.server.timed.AsyncTimed)")
  public void asyncTimedAnnotationPointcut() {
  }

  @Around("asyncTimedAnnotationPointcut()")
  public Object methodsAnnotatedWithAsyncTime(final ProceedingJoinPoint joinPoint) throws Throwable {
    return proceed(joinPoint);
  }

  private Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
    final Object result = joinPoint.proceed();
    if (!isCompletableFuture(result)) {
      return result;
    }

    final String description = joinPoint.toString();
    final StopWatch watch = new StopWatch();
    watch.start();

    String initiatingThread = Thread.currentThread().getName();
    return ((CompletableFuture<?>) result).thenApply(__ -> {
      String executingThread = Thread.currentThread().getName();
      watch.stop();
      LOG.info("Timed: " + description + " ; " + watch.toString() + " ; Initiating Thread '" + initiatingThread
        + "' Executing Thread '" + executingThread + "'.");
      System.out.println("Timed: " + description + " ; " + watch.toString());
      return __;
    });
  }

  private boolean isCompletableFuture(Object result) {
    return CompletableFuture.class.isAssignableFrom(result.getClass());
  }
}