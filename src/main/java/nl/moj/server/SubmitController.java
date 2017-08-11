package nl.moj.server;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import nl.moj.server.compile.CompileResult;
import nl.moj.server.compile.CompileService;
import nl.moj.server.model.Result;
import nl.moj.server.persistence.ResultMapper;
import nl.moj.server.test.TestResult;
import nl.moj.server.test.TestService;

@Controller
@MessageMapping("/submit")
public class SubmitController {

	@Autowired
	private CompileService compileService;

	@Autowired
	private TestService testService;

	@Autowired
	private ResultMapper resultMapper;
	
	@MessageMapping("/compile")
	@SendToUser("/queue/feedback")
	public FeedbackMessage compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		System.out.println(message.getSource());
		CompletableFuture<CompileResult> future = compileService.compile(Arrays.asList(message.getSource()));
		
		List<Result> allResults = resultMapper.getAllResults();
		
		String feedback = future.get().getCompileResult() + allResults.get(0).getResult();
		return new FeedbackMessage(user.getName(), feedback, time);
	}

	@MessageMapping("/test")
	@SendToUser("/queue/feedback")
	public FeedbackMessage test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg)
			throws Exception {
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		CompletableFuture<CompileResult> future = compileService.compile(Arrays.asList(message.getSource()));
		CompletableFuture<TestResult> testFuture = future.thenCompose(compileResult -> testService.test(compileResult));
		testFuture.thenAccept(p -> System.out.println(p.getTestResult()));
		String feedback = future.get().getCompileResult() + testFuture.get().getTestResult();
		return new FeedbackMessage(user.getName(), feedback, time);
	}
}
