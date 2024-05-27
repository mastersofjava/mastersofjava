/*
   Copyright 2020 First Eight BV (The Netherlands)


   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.bootstrap.controller;

import java.io.IOException;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.bootstrap.BootstrapService;
import nl.moj.server.authorization.Role;

@Controller
@AllArgsConstructor
@Slf4j
public class BootstrapController {

    private final BootstrapService bootstrapService;

    @GetMapping("/bootstrap")
    public String bootstrap(HttpServletRequest request) {
        if (bootstrapService.isBootstrapNeeded()) {
            return "bootstrap";
        }
        return determineRedirect(request);
    }

    @RolesAllowed({ Role.ADMIN })
    @PostMapping("/bootstrap")
    public String doBootstrap(RedirectAttributes redirectAttributes) {
        if (bootstrapService.isBootstrapNeeded()) {
            try {
                bootstrapService.bootstrap();
            } catch (IOException ioe) {
                log.error("Bootstrap failed: {}", ioe.getMessage(), ioe);
                return redirectFailure(redirectAttributes, "Bootstrap failed, see console logs for more information.");
            }
        }
        return "redirect:/control";
    }

    private String determineRedirect(HttpServletRequest request) {
        String redirectUri = request.getHeader(HttpHeaders.REFERER);
        if (StringUtils.isBlank(redirectUri) || redirectUri.endsWith("/bootstrap")) {
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
