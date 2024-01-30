package nl.moj.server;

import org.springframework.stereotype.Controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@AllArgsConstructor
@Slf4j
public class ErrorController {
    //	@GetMapping("/error")
    //	public String error(Model model, HttpServletRequest request) {
    //		log.warn("An error occurred request: {} model: {}", request.getRequestURI(), model);
    //		return "error";
    //	}
}
