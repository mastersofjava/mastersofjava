package nl.moj.server;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import nl.moj.server.compile.CompileService;

@Controller
@MessageMapping("/submit")
public class SubmitController {

	
	@Autowired
	private CompileService compileService;
	
	@MessageMapping("/compile")
	//@SendTo("/topic/messages")
	@SendToUser("/queue/feedback")
	public FeedbackMessage compile(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg) throws Exception {
	    String time = new SimpleDateFormat("HH:mm").format(new Date());
	    String result = compileService.compile(message.getSource());
	    return new FeedbackMessage(user.getName(), result, time);
	}
	
	
	
	@MessageMapping("/test")
	@SendTo("/feedback")
	//@SendToUser("/feedback")
	public FeedbackMessage test(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg) throws Exception {
	    String time = new SimpleDateFormat("HH:mm").format(new Date());
	    System.out.println(message.getSource());
	    System.out.println(user.getName());
	    return new FeedbackMessage(message.getTeam(), message.getSource(), time);
	}
//	
//	@MessageMapping("/all")
//	@SendTo("/topic/messages")
//	@SendToUser("/feedback")
//	public FeedbackMessage all(SourceMessage message, @AuthenticationPrincipal Principal user, MessageHeaders mesg) throws Exception {
//	    String time = new SimpleDateFormat("HH:mm").format(new Date());
//	    System.out.println(message.getSource());
//	    System.out.println(user.getName());
//	    return new FeedbackMessage(message.getTeam(), message.getSource(), time);
//	}
}
