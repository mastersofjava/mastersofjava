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
package nl.moj.server.runtime;

import java.util.UUID;

import nl.moj.server.message.model.operations.CompileOperation;
import nl.moj.server.message.model.operations.TestOperation;
import nl.moj.server.message.service.JMSService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JMSTest {

    @Autowired
    private JMSService jmsService;

    @Test
    public void testJms() {
        jmsService.sendCompileOperation(CompileOperation.builder()
                .uuid(UUID.randomUUID())
                .message("Testing compile ....")
                .build());

        jmsService.sendTestOperation(TestOperation.builder()
                .uuid(UUID.randomUUID())
                .message("Testing test ....")
                .build());
    }
}
