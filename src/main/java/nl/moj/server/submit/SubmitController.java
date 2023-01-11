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
package nl.moj.server.submit;

import java.security.Principal;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.submit.model.SourceMessage;

@Controller
@MessageMapping("/submit")
@Slf4j
@AllArgsConstructor
public class SubmitController {

    private SubmitFacade submitFacade;

    @MessageMapping("/compile")
    public void compile(SourceMessage message, Principal principal, MessageHeaders headers)
            throws Exception {
        try {
            submitFacade.registerCompileRequest(message,principal);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @MessageMapping("/test")
    public void test(SourceMessage message, Principal principal, MessageHeaders headers)
            throws Exception {
        try {
            submitFacade.registerTestRequest(message,principal);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @MessageMapping("/submit")
    public void submit(SourceMessage message, Principal principal, MessageHeaders headers)
            throws Exception {
        try {
            submitFacade.registerSubmitRequest(message,principal);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }



    // TODO this should not be here

}
