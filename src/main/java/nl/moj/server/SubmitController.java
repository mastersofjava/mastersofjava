package nl.moj.server;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import net.tascalate.concurrent.CompletableTask;
import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.CompileService;
import nl.moj.server.test.TestResult;
import nl.moj.server.test.TestService;

@Controller
@MessageMapping("/submit")
public class SubmitController {

	private static final Logger log = LoggerFactory.getLogger(SubmitController.class);

	@Autowired
	private CompileService compileService;

	@Autowired
	private TestService testService;

	@Autowired
	@Qualifier("timed")
	private Executor timed;

	@Autowired
	private SimpMessagingTemplate template;

	@MessageMapping("/compile")
	public void compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		CompletableTask.supplyAsync(compileService.compile(message.getSource(), user.getName()), timed)
				.thenAccept(testResult -> sendFeedbackMessage(testResult)).get();
	}

	@MessageMapping("/test")
	public void test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		message.getSource().forEach((k,v) -> log.info("{},{}",k,v) );
		CompletableTask.supplyAsync(compileService.compile(message.getSource(), user.getName(), true), timed)
				.thenComposeAsync( compileResult -> testService.test(compileResult),timed)
				.thenAccept(testResult -> sendFeedbackMessage(testResult)).get();
	}

	private void sendFeedbackMessage(CompileResult compileResult) {
		log.info("sending feedback");
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		template.convertAndSendToUser(compileResult.getUser(), "/queue/feedback",
				new FeedbackMessage(compileResult.getUser(), compileResult.getCompileResult(), time));
	}

	private void sendFeedbackMessage(TestResult testResult) {
		log.info("sending feedback");
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		template.convertAndSendToUser(testResult.getUser(), "/queue/feedback",
				new FeedbackMessage(testResult.getUser(), testResult.getTestResult(), time));
	}

}
