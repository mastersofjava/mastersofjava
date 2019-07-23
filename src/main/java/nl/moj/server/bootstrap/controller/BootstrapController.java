package nl.moj.server.bootstrap.controller;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.bootstrap.service.BootstrapService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@Slf4j
public class BootstrapController {

    private final BootstrapService bootstrapService;

    @GetMapping("/bootstrap")
    public String bootstrap() {
        if (bootstrapService.isBootstrapNeeded()) {
            return "bootstrap";
        }
        return "redirect:/";
    }

    @PostMapping("/bootstrap")
    public String doBootstrap(@ModelAttribute("form") BootstrapForm form, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        if (bootstrapService.isBootstrapNeeded()) {
            if (form.getPassword1().equals(form.getPassword2())) {
                try {
                    bootstrapService.bootstrap(form.getUsername(), form.getPassword1());
                } catch (IOException ioe) {
                    log.error("Bootstrap failed.", ioe);
                    return redirectFailure(redirectAttributes, "Bootstrap failed, see console logs for more information.");
                }
            } else {
                return redirectFailure(redirectAttributes, "Passwords do not match.");
            }
        }
        return determineRedirect(request);
    }

    private String determineRedirect(HttpServletRequest request) {
        String redirectUri = request.getHeader(HttpHeaders.REFERER);
        if (StringUtils.isBlank(redirectUri)) {
            redirectUri = "/";
        }
        return "redirect:" + redirectUri;
    }

    private String redirectFailure(RedirectAttributes redirectAttributes, String msg) {
        redirectAttributes.addFlashAttribute("error", true);
        redirectAttributes.addFlashAttribute("msg", msg);
        return "redirect:/bootstrap";
    }
}
