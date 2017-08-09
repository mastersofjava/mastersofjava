package nl.moj.server;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.CompileService;
import nl.moj.server.compile.TestResult;
import nl.moj.server.compile.TestService;

@Controller
@MessageMapping("/submit")
public class SubmitController {

	@Autowired
	private CompileService compileService;

	@Autowired
	private TestService testService;

	@MessageMapping("/compile")
	@SendToUser("/queue/feedback")
	public FeedbackMessage compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		CompletableFuture<CompileResult> future = compileService.compile(message.getSource());
		String feedback = future.get().getCompileResult();
		return new FeedbackMessage(user.getName(), feedback, time);
	}

	@MessageMapping("/test")
	@SendToUser("/queue/feedback")
	public FeedbackMessage test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		CompletableFuture<CompileResult> future = compileService.compile(message.getSource());
		CompletableFuture<TestResult> testFuture = future.thenCompose(compileResult -> testService.test(compileResult));
		testFuture.thenAccept(p -> System.out.println(p.getTestResult()));
		String feedback = future.get().getCompileResult() + testFuture.get().getTestResult();
		return new FeedbackMessage(user.getName(), feedback, time);
	}
}
