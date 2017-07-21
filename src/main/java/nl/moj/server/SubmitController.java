package nl.moj.server;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class SubmitController {

	@MessageMapping("/chat")
	@SendTo("/topic/messages")
	public FeedbackMessage send(SourceMessage message, @AuthenticationPrincipal Principal user) throws Exception {
	    String time = new SimpleDateFormat("HH:mm").format(new Date());
	    System.out.println(message.getSource());
	    System.out.println(user.getName());
	    return new FeedbackMessage(message.getTeam(), message.getSource(), time);
	}
}
