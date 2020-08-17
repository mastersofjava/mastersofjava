package nl.moj.server.login;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * Startpage Master of Java
 */
@Slf4j
@Controller
@AllArgsConstructor
public class HomeController {

    @GetMapping("/index")
    public String index() {
        return "index";
    }

}
