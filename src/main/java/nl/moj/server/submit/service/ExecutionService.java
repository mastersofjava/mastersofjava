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
package nl.moj.server.submit.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.ExecutionModel;
import nl.moj.server.compiler.service.CompileRequest;
import nl.moj.server.compiler.service.CompileResult;
import nl.moj.server.compiler.service.CompileService;
import nl.moj.server.runtime.model.ActiveAssignment;
import nl.moj.server.runtime.model.AssignmentFile;
import nl.moj.server.test.service.TestRequest;
import nl.moj.server.test.service.TestResults;
import nl.moj.server.test.service.TestService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExecutionService {

    private final CompileService compileService;
    private final TestService testService;
    private final Executor sequential;
    private final Executor parallel;

    public ExecutionService(@Qualifier("sequential") Executor sequential, @Qualifier("parallel") Executor parallel,
                            CompileService compileService, TestService testService) {
        this.sequential = sequential;
        this.parallel = parallel;
        this.compileService = compileService;
        this.testService = testService;
    }

    public CompletableFuture<CompileResult> compile(SubmitRequest submitRequest, ActiveAssignment activeAssignment) {
        return compileService.scheduleCompile(CompileRequest.from(submitRequest), getExecutor(activeAssignment), activeAssignment);
    }

    public CompletableFuture<TestResults> test(SubmitRequest submitRequest, List<AssignmentFile> tests, ActiveAssignment activeAssignment) {
        return testService.scheduleTests(TestRequest.from(submitRequest), tests, getExecutor(activeAssignment), activeAssignment);
    }

    private Executor getExecutor(ActiveAssignment activeAssignment) {
        if (activeAssignment == null) {
            log.debug("Executing assignment in sequentially, can slow down client response (only used by admin)");
            return sequential;
        }
        if (activeAssignment.getExecutionModel() == ExecutionModel.SEQUENTIAL) {
        	log.debug("Executing assignment sequentially, can slow down response to client");
            return sequential;
        }
        log.debug("Executing assignment in parallel, can impact timing of assignment");
        return parallel;
    }

}
